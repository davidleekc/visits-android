package com.hypertrack.android.interactors

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fonfon.kgeohash.GeoHash
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.models.Integration
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.repository.*
import com.hypertrack.android.ui.base.Consumable
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.sync.Mutex

interface PlacesInteractor {
    val errorFlow: MutableSharedFlow<Consumable<Exception>>
    val geofences: LiveData<Map<String, LocalGeofence>>
    val geofencesDiff: Flow<List<LocalGeofence>>
    val isLoadingForLocation: MutableLiveData<Boolean>

    fun loadGeofencesForMap(center: LatLng)
    fun getGeofence(geofenceId: String): LocalGeofence
    fun invalidateCache()
    suspend fun createGeofence(
        latitude: Double,
        longitude: Double,
        name: String?,
        address: String?,
        description: String?,
        integration: Integration?
    ): CreateGeofenceResult

    suspend fun loadPage(pageToken: String?): GeofencesPage
}

class PlacesInteractorImpl(
    private val placesRepository: PlacesRepository,
    private val integrationsRepository: IntegrationsRepository,
    private val globalScope: CoroutineScope
) : PlacesInteractor {

    override val errorFlow = MutableSharedFlow<Consumable<Exception>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val _geofences = mutableMapOf<String, LocalGeofence>()
    override val geofences = MutableLiveData<Map<String, LocalGeofence>>(mapOf())
    override val geofencesDiff = MutableSharedFlow<List<LocalGeofence>>(
        replay = Int.MAX_VALUE,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    private val pageCache = mutableMapOf<String?, List<LocalGeofence>>()
    private val geoCache = GeoCache()
    private val mutex = Mutex()

    val debugCacheState = MutableLiveData<List<GeoCacheItem>>()

    override val isLoadingForLocation = MutableLiveData<Boolean>(false)
    private var firstPageJob: Deferred<GeofencesPage>? = null

    init {
        firstPageJob = globalScope.async {
            loadPlacesPage(null, true)
        }
    }

    override suspend fun loadPage(pageToken: String?): GeofencesPage {
        return loadPlacesPage(pageToken, false)
    }

    override fun loadGeofencesForMap(center: LatLng) {
        val gh = GeoHash(center.latitude, center.longitude, 5)
        globalScope.launch(Dispatchers.Main) {
            loadRegion(gh)
            gh.adjacent.forEach { loadRegion(it) }
        }
    }

    override fun getGeofence(geofenceId: String): LocalGeofence {
        return geofences.value!!.getValue(geofenceId)
    }

    override fun invalidateCache() {
        firstPageJob?.cancel()
        firstPageJob = null
        integrationsRepository.invalidateCache()
        _geofences.clear()
        geofences.postValue(_geofences)
        pageCache.clear()
        geoCache.clear()
    }

    override suspend fun createGeofence(
        latitude: Double,
        longitude: Double,
        name: String?,
        address: String?,
        description: String?,
        integration: Integration?
    ): CreateGeofenceResult {
        return withContext(globalScope.coroutineContext) {
            placesRepository.createGeofence(
                latitude,
                longitude,
                name,
                address,
                description,
                integration
            ).apply {
                if (this is CreateGeofenceSuccess) {
                    addGeofencesToCache(listOf(geofence))
                }
            }
        }
    }

    private suspend fun loadPlacesPage(pageToken: String?, initial: Boolean): GeofencesPage {
        if (pageCache.containsKey(pageToken)) {
//            Log.v("hypertrack-verbose", "cached: ${pageToken.hashCode()}")
            return GeofencesPage(
                pageCache.getValue(pageToken),
                pageToken
            )
        } else {
            if ((firstPageJob?.let { it.isActive || it.isCompleted } == true) && !initial) {
//                Log.v("hypertrack-verbose", "waiting first job: ${pageToken.hashCode()}")
                return firstPageJob!!.await()
            } else {
//                Log.v("hypertrack-verbose", "loading: ${pageToken.hashCode()}")
                return placesRepository.loadPage(pageToken).apply {
                    pageCache[pageToken] = this.geofences
                    addGeofencesToCache(geofences)
                }.apply {
//                    Log.v("hypertrack-verbose", "--loaded: ${pageToken.hashCode()}")
                }
            }
        }
    }

    private fun loadRegion(gh: GeoHash) {
        //todo retry on error
        if (!geoCache.contains(gh)) {
            isLoadingForLocation.postValue(true)
            geoCache.add(gh)
        }
    }

    private fun addGeofencesToCache(newPack: List<LocalGeofence>) {
        newPack.forEach {
            _geofences.put(it.id, it)
        }
        geofences.postValue(_geofences)
        updateDebugCacheState()
        globalScope.launch {
            geofencesDiff.emit(newPack)
        }
    }

    private fun updateDebugCacheState() {
        debugCacheState.postValue(geoCache.getItems().values.toList())
    }

    inner class GeoCache {
        private val items = mutableMapOf<GeoHash, GeoCacheItem>()

        fun getItems(): Map<GeoHash, GeoCacheItem> = items

        fun clear() {
            items.forEach { it.value.job.cancel() }
            items.clear()
        }

        fun contains(gh: GeoHash): Boolean {
            return items.contains(gh)
        }

        fun add(gh: GeoHash) {
            items[gh] = GeoCacheItem(gh)
            updateDebugCacheState()
        }

        fun isLoading(): Boolean {
            return items.values.any { it.status == Status.LOADING }
        }

    }

    inner class GeoCacheItem(
        val gh: GeoHash
    ) {
        val job: Job
        var paginator: Paginator<List<LocalGeofence>>? = null

        var status: Status = Status.LOADING

        init {
            job = globalScope.launch(Dispatchers.Main) {
                try {
//                    Log.v("hypertrack-verbose", "Loading for ${gh}")
                    paginator = object : Paginator<List<LocalGeofence>>() {
                        override suspend fun load(pageToken: String?): PageRes<List<LocalGeofence>> {
                            return placesRepository.loadPage(pageToken, gh).let {
                                PageRes(it.geofences, it.paginationToken)
                            }
                        }

                        override fun onLoaded(
                            pageToken: String?,
                            res: List<LocalGeofence>
                        ) {
//                            Log.v("hypertrack-verbose", "page $pageToken for ${gh}")
                            addGeofencesToCache(res)
                        }
                    }.apply {
                        start()
                    }

//                    Log.v(
//                        "hypertrack-verbose",
//                        geoCache.getItems().map { it.second.isLoading() }.toString()
//                    )
                    status = Status.COMPLETED
                    isLoadingForLocation.postValue(geoCache.isLoading())
                    Log.v(
                        "hypertrack-verbose",
                        "${gh}: loaded"
                    )
                } catch (e: Exception) {
                    status = Status.ERROR
//                    loadingGeohashes.remove(gh)
                    isLoadingForLocation.postValue(geoCache.isLoading())
                    errorFlow.emit(Consumable(e))
                }
            }
        }

    }

    enum class Status {
        COMPLETED, LOADING, ERROR
    }

}

abstract class Paginator<T> {
    var pageToken: String? = null

    suspend fun start() {
        do {
            val res = load(pageToken)
            onLoaded(pageToken, res.res)
            pageToken = res.pageToken
        } while (pageToken != null)
    }

    abstract suspend fun load(pageToken: String?): PageRes<T>

    abstract fun onLoaded(pageToken: String?, res: T)

    inner class PageRes<R>(
        val res: R,
        val pageToken: String?
    )
}




