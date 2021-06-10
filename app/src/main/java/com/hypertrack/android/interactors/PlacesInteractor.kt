package com.hypertrack.android.interactors

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.fonfon.kgeohash.GeoHash
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.models.Integration
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.repository.*
import com.hypertrack.android.ui.base.Consumable
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

interface PlacesInteractor {
    val errorFlow: MutableSharedFlow<Consumable<Exception>>
    val geofences: MutableLiveData<Map<String, LocalGeofence>>
    val isLoadingForLocation: MutableLiveData<Boolean>

    fun loadGeofencesForLocation(latLng: LatLng)
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
    override val geofences: MutableLiveData<Map<String, LocalGeofence>> =
        MutableLiveData<Map<String, LocalGeofence>>(mapOf())
    private val pageCache = mutableMapOf<String?, List<LocalGeofence>>()

    override val isLoadingForLocation = MutableLiveData<Boolean>(false)
    private var firstPageJob: Deferred<GeofencesPage>? = null

    private val loadedGeohashes = mutableSetOf<GeoHash>()
    private val loadingGeohashes = mutableSetOf<GeoHash>()

    init {
        firstPageJob = globalScope.async {
            loadPlacesPage(null, true)
        }
    }

    override suspend fun loadPage(pageToken: String?): GeofencesPage {
        return loadPlacesPage(pageToken, false)
    }

    private suspend fun loadPlacesPage(pageToken: String?, initial: Boolean): GeofencesPage {
        if (pageCache.containsKey(pageToken)) {
            Log.v("hypertrack-verbose", "cached: ${pageToken.hashCode()}")
            return GeofencesPage(
                pageCache.getValue(pageToken),
                pageToken
            )
        } else {
            if ((firstPageJob?.let { it.isActive || it.isCompleted } == true) && !initial) {
                Log.v("hypertrack-verbose", "waiting first job: ${pageToken.hashCode()}")
                return firstPageJob!!.await()
            } else {
                Log.v("hypertrack-verbose", "loading: ${pageToken.hashCode()}")
                return placesRepository.loadPage(pageToken).apply {
                    pageCache[pageToken] = this.geofences
                    addGeofencesToCache(geofences)
                }.apply {
                    Log.v("hypertrack-verbose", "--loaded: ${pageToken.hashCode()}")
                }
            }
        }
    }

    override fun loadGeofencesForLocation(latLng: LatLng) {
        //todo task pagination
        val gh = GeoHash(latLng.latitude, latLng.longitude, 4)
        if (!loadedGeohashes.contains(gh) && !loadingGeohashes.contains(gh)) {
            isLoadingForLocation.postValue(true)
            loadingGeohashes.add(gh)
            Log.v("hypertrack-verbose", "Loading for ${gh}")
            globalScope.launch {
                try {
                    val res = placesRepository.loadPage(null, gh)
                    addGeofencesToCache(res.geofences)
                    loadedGeohashes.add(gh)
                    loadingGeohashes.remove(gh)
                    isLoadingForLocation.postValue(loadingGeohashes.size != 0)
                    Log.v(
                        "hypertrack-verbose",
                        "${gh}: loaded ${loadingGeohashes} ${loadedGeohashes}"
                    )
                } catch (e: Exception) {
                    loadingGeohashes.remove(gh)
                    isLoadingForLocation.postValue(loadingGeohashes.size != 0)
                    errorFlow.emit(Consumable(e))
                }
            }
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
        loadedGeohashes.clear()
        loadingGeohashes.clear()
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

    private fun addGeofencesToCache(newPack: List<LocalGeofence>) {
        geofences.postValue(geofences.value!!.toMutableMap().apply {
            newPack.forEach {
                put(it.id, it)
            }
        })
    }

}



