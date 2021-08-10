package com.hypertrack.android.interactors

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fonfon.kgeohash.GeoHash
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.delegates.GeofenceNameDelegate
import com.hypertrack.android.models.Integration
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.repository.*
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

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
        radius: Int?,
        description: String?,
        integration: Integration?
    ): CreateGeofenceResult

    suspend fun loadPage(pageToken: String?): GeofencesPage

    suspend fun hasAdjacentGeofence(latLng: LatLng, radius: Int? = null): Boolean

    companion object {
        const val DEFAULT_RADIUS_METERS = 100
    }
}

class PlacesInteractorImpl(
    private val placesRepository: PlacesRepository,
    private val integrationsRepository: IntegrationsRepository,
    private val osUtilsProvider: OsUtilsProvider,
    private val globalScope: CoroutineScope
) : PlacesInteractor {

    private val geofenceNameDelegate = GeofenceNameDelegate(osUtilsProvider)

    private var pendingCreatedGeofences = mutableListOf<LocalGeofence>()

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
        val gh = GeoHash(center.latitude, center.longitude, 4)
        globalScope.launch(Dispatchers.Main) {
            loadRegion(gh)
            gh.adjacent.forEach { loadRegion(it) }
        }
    }

    override fun getGeofence(geofenceId: String): LocalGeofence {
        return geofences.value!!.getValue(geofenceId)
    }

    override suspend fun hasAdjacentGeofence(latLng: LatLng, radius: Int?): Boolean {
        return geofences.value!!.any { (_, geofence) ->
            val distance = osUtilsProvider.distanceMeters(geofence.latLng, latLng)
            //todo check radius null

            return@any geofence.radius?.let {
                distance < geofence.radius + (radius
                    ?: PlacesInteractor.DEFAULT_RADIUS_METERS)
            } ?: false
        }
    }

    override fun invalidateCache() {
        invalidatePageCache()
        integrationsRepository.invalidateCache()
        _geofences.clear()
        geofences.postValue(_geofences)
        geoCache.clear()
    }

    private fun invalidatePageCache() {
        pendingCreatedGeofences.clear()
        firstPageJob?.cancel()
        firstPageJob = null
        pageCache.clear()
    }

    override suspend fun createGeofence(
        latitude: Double,
        longitude: Double,
        name: String?,
        address: String?,
        radius: Int?,
        description: String?,
        integration: Integration?
    ): CreateGeofenceResult {
        return withContext(globalScope.coroutineContext) {
            placesRepository.createGeofence(
                latitude = latitude,
                longitude = longitude,
                name = name,
                address = address,
                radius = radius ?: PlacesInteractor.DEFAULT_RADIUS_METERS,
                description = description,
                integration = integration
            ).apply {
                if (this is CreateGeofenceSuccess) {
                    addGeofencesToCache(listOf(geofence))
                    pendingCreatedGeofences.add(geofence)
                }
            }
        }
    }

    private suspend fun loadPlacesPage(pageToken: String?, initial: Boolean): GeofencesPage {
        if (pageCache.containsKey(pageToken)) {
//            Log.v("hypertrack-verbose", "cached: ${pageToken.hashCode()}")
            return GeofencesPage(
                pageCache.getValue(pageToken).let {
                    if (pageToken == null && pendingCreatedGeofences.isNotEmpty()) {
                        pendingCreatedGeofences.map {
                            it.copy(
                                name = "${geofenceNameDelegate.getName(it)} (${
                                    osUtilsProvider.stringFromResource(
                                        R.string.places_recently_created
                                    )
                                })"
                            )
                        }.plus(it)
                    } else it
                },
                pageToken
            )
        } else {
            val res =
                if ((firstPageJob?.let { it.isActive || it.isCompleted } == true) && !initial) {
//                    Log.v("hypertrack-verbose", "waiting first job: ${pageToken.hashCode()}")
                    firstPageJob!!.await()
                } else {
//                    Log.v("hypertrack-verbose", "loading: ${pageToken.hashCode()}")
                    placesRepository.loadGeofencesPage(pageToken).apply {
                        pageCache[pageToken] = this.geofences
                        addGeofencesToCache(geofences)
                    }.apply {
//                    Log.v("hypertrack-verbose", "--loaded: ${pageToken.hashCode()}")
                    }
                }
            if (pageToken == null) {
                pendingCreatedGeofences.clear()
            }
            return res
        }
    }

    private fun loadRegion(gh: GeoHash) {
        val item = geoCache.getItems()[gh]
//        Log.v(
//            "hypertrack-verbose",
//            geoCache.getItems().values.map { "${it.gh} ${it.status}" }.toString()
//        )
        when (item?.status) {
            Status.COMPLETED, Status.LOADING -> {
            }
            null -> {
//                Log.v("hypertrack-verbose", "loading: ${gh}")
                isLoadingForLocation.postValue(true)
                geoCache.add(gh)
            }
            Status.ERROR -> {
//                Log.v("hypertrack-verbose", "retrying: ${gh}")
                isLoadingForLocation.postValue(true)
                item.retry()
            }
        }
    }

    private fun addGeofencesToCache(newPack: List<LocalGeofence>) {
        globalScope.launch(Dispatchers.Main) {
            newPack.forEach {
                _geofences.put(it.id, it)
            }
            geofences.postValue(_geofences)
            updateDebugCacheState()
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
        lateinit var job: Job
        var pageToken: String? = null
        lateinit var status: Status

        init {
            load()
        }

        private fun load() {
            status = Status.LOADING
            job = globalScope.launch(Dispatchers.Main) {
                try {
                    do {
                        val res = placesRepository.loadGeofencesPage(pageToken, gh)
                        pageToken = res.paginationToken
                        addGeofencesToCache(res.geofences)
                    } while (pageToken != null)

                    status = Status.COMPLETED
                    isLoadingForLocation.postValue(geoCache.isLoading())
//                    Log.v(
//                        "hypertrack-verbose",
//                        "${gh}: loaded"
//                    )
                } catch (e: Exception) {
//                    Log.v("hypertrack-verbose", "error for ${gh} :  ${e.format()}")
                    status = Status.ERROR
                    isLoadingForLocation.postValue(geoCache.isLoading())
                    errorFlow.emit(Consumable(e))
                }
            }
        }

        fun retry() {
            load()
        }
    }

    enum class Status {
        COMPLETED, LOADING, ERROR
    }

}