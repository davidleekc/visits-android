package com.hypertrack.android.interactors

import com.hypertrack.android.api.GeofenceVisit
import com.hypertrack.android.models.local.LocalGeofenceVisit
import com.hypertrack.android.repository.PlacesRepository
import com.hypertrack.android.ui.common.DataPage

interface PlacesVisitsInteractor {
    suspend fun loadPage(pageToken: String?): DataPage<LocalGeofenceVisit>
    fun invalidateCache()
}

class PlacesVisitsInteractorImpl(
    private val placesRepository: PlacesRepository
) : PlacesVisitsInteractor {

    override suspend fun loadPage(pageToken: String?): DataPage<LocalGeofenceVisit> {
        return placesRepository.loadAllGeofencesVisitsPage(pageToken)
    }

    override fun invalidateCache() {

    }
}