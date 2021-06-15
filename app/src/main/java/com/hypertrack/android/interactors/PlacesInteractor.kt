package com.hypertrack.android.interactors

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.fonfon.kgeohash.GeoHash
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.VisibleRegion
import com.hypertrack.android.models.Integration
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.repository.*
import com.hypertrack.android.ui.base.Consumable
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface PlacesInteractor {
    val errorFlow: MutableSharedFlow<Consumable<Exception>>
    val geofences: MutableLiveData<Map<String, LocalGeofence>>
    val isLoadingForLocation: MutableLiveData<Boolean>

    fun loadGeofencesForMap(center: LatLng, region: VisibleRegion)
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

    //todo task check oom
    override val geofences: MutableLiveData<Map<String, LocalGeofence>> =
        MutableLiveData<Map<String, LocalGeofence>>(mapOf())
    private val geofencesMutex = Mutex()
    private val pageCache = mutableMapOf<String?, List<LocalGeofence>>()
    val geoCache = GeoCache()

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

    override fun loadGeofencesForMap(center: LatLng, region: VisibleRegion) {
        //todo task pagination
        globalScope.launch {
            val gh = GeoHash(center.latitude, center.longitude, 5)
            loadRegion(gh)
//            gh.adjacent.forEach { loadRegion(it) }
        }
    }

    override fun getGeofence(geofenceId: String): LocalGeofence {
        return geofences.value!!.getValue(geofenceId)
    }

    override fun invalidateCache() {
        firstPageJob?.cancel()
        firstPageJob = null
        integrationsRepository.invalidateCache()
        geofences.postValue(mapOf())
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

    private suspend fun loadRegion(gh: GeoHash) {
        //todo retry on error
        if (!geoCache.contains(gh)) {
            isLoadingForLocation.postValue(true)
            globalScope.launch {
                geoCache.add(gh)
            }
        }
    }

    private suspend fun addGeofencesToCache(newPack: List<LocalGeofence>) {
        geofencesMutex.withLock {
            geofences.postValue(geofences.value!!.toMutableMap().apply {
                newPack.forEach {
                    put(it.id, it)
                }
            })
        }
    }

    inner class GeoCache {
        private val items = mutableMapOf<GeoHash, GeoCacheItem>()
        private val mutex = Mutex()

        suspend fun getItems() = mutex.withLock {
            items.map { Pair<GeoHash, GeoCacheItem>(it.key, it.value) }
        }

        fun clear() {
            globalScope.launch {
                mutex.withLock {
                    items.forEach { it.value.job.cancel() }
                    items.clear()
                }
            }
        }

        suspend fun contains(gh: GeoHash): Boolean {
            mutex.withLock {
                return items.contains(gh)
            }
        }

        suspend fun add(gh: GeoHash) {
            mutex.withLock {
                items.put(gh, GeoCacheItem(gh))
            }
        }

        suspend fun isLoading(): Boolean {
            mutex.withLock {
                return items.values.any { it.isLoading() }
            }
        }
    }

    inner class GeoCacheItem(
        val gh: GeoHash
    ) {
        val job: Job
        var paginator: Paginator<List<LocalGeofence>>? = null

        private var loaded: Boolean = false

        init {
            job = globalScope.launch {
                try {
                    Log.v("hypertrack-verbose", "Loading for ${gh}")
                    paginator = object : Paginator<List<LocalGeofence>>() {
                        override suspend fun load(pageToken: String?): PageRes<List<LocalGeofence>> {
                            return placesRepository.loadPage(pageToken, gh).let {
                                PageRes(it.geofences, it.paginationToken)
                            }
                        }

                        override suspend fun onLoaded(
                            pageToken: String?,
                            res: List<LocalGeofence>
                        ) {
                            Log.v("hypertrack-verbose", "page $pageToken for ${gh}")
                            addGeofencesToCache(res)
                        }
                    }.apply {
                        start()
                    }

//                    Log.v(
//                        "hypertrack-verbose",
//                        geoCache.getItems().map { it.second.isLoading() }.toString()
//                    )
                    loaded = true
                    isLoadingForLocation.postValue(geoCache.isLoading())
                    Log.v(
                        "hypertrack-verbose",
                        "${gh}: loaded"
                    )
                } catch (e: Exception) {
//                    loadingGeohashes.remove(gh)
                    isLoadingForLocation.postValue(geoCache.isLoading())
                    errorFlow.emit(Consumable(e))
                }
            }
        }

        fun isLoading(): Boolean {
            return !loaded
        }
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

    abstract suspend fun onLoaded(pageToken: String?, res: T)

    inner class PageRes<R>(
        val res: R,
        val pageToken: String?
    )
}




