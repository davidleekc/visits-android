package com.hypertrack.android.interactors

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.Geofence
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.repository.GeofencesPage
import com.hypertrack.android.repository.IntegrationsRepository
import com.hypertrack.android.repository.PlacesRepository

class PlacesInteractor(
    private val placesRepository: PlacesRepository,
    private val integrationsRepository: IntegrationsRepository,
) : PlacesRepository by placesRepository {

    val geofences: MutableLiveData<Map<String, LocalGeofence>> =
        MutableLiveData<Map<String, LocalGeofence>>(mapOf())

    override suspend fun loadPage(pageToken: String?): GeofencesPage {
        return placesRepository.loadPage(pageToken).apply {
            addGeofencesToCache(geofences)
        }
    }

    fun loadGeofencesForLocation(latLng: LatLng) {

    }

    fun getGeofence(geofenceId: String): LocalGeofence {
        return geofences.value!!.getValue(geofenceId)
    }

    fun refresh() {
        integrationsRepository.invalidateCache()
    }

    private fun addGeofencesToCache(newPack: List<LocalGeofence>) {
        geofences.postValue(geofences.value!!.toMutableMap().apply {
            newPack.forEach {
                put(it.id, it)
            }
        })
    }

}



