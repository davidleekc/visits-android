package com.hypertrack.android.interactors

import com.hypertrack.android.api.GeofenceMarker
import com.hypertrack.android.repository.GeofencesPage
import com.hypertrack.android.repository.PlacesRepository
import com.hypertrack.android.ui.common.DataPage

interface PlacesVisitsInteractor {
    suspend fun loadPage(pageToken: String?): DataPage<GeofenceMarker>
    fun invalidateCache()
}

class PlacesVisitsInteractorImpl(
    private val placesRepository: PlacesRepository
) : PlacesVisitsInteractor {

    override suspend fun loadPage(pageToken: String?): DataPage<GeofenceMarker> {
        return placesRepository.loadAllGeofencesVisitsPage(pageToken)
    }

    override fun invalidateCache() {

    }
}