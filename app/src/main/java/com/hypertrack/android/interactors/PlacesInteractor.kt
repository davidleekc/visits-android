package com.hypertrack.android.interactors

import com.hypertrack.android.api.Geofence
import com.hypertrack.android.repository.PlacesRepository

class PlacesInteractor(
    private val placesRepository: PlacesRepository
) : PlacesRepository by placesRepository



