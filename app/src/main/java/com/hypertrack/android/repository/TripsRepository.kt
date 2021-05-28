package com.hypertrack.android.repository

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.ApiClient
import com.hypertrack.android.api.TripParams
import com.hypertrack.android.models.CreateTripError
import com.hypertrack.android.models.ShareableTripSuccess
import com.hypertrack.android.utils.HyperTrackService

interface TripsRepository {

    suspend fun createTrip(latLng: LatLng): TripCreationResult
}

class TripsRepositoryImpl(
    private val apiClient: ApiClient,
    private val hyperTrackService: HyperTrackService
) : TripsRepository {

    override suspend fun createTrip(latLng: LatLng): TripCreationResult {
        return when (val result = apiClient.createTrip(
            TripParams(
                hyperTrackService.deviceId,
                latitude = latLng.latitude,
                longitude = latLng.longitude,
            )
        )) {
            is ShareableTripSuccess -> {
                TripCreationSuccess()
            }
            is CreateTripError -> {
                TripCreationError(((result.error ?: UnknownError()) as Exception))
            }
        }
    }

}

sealed class TripCreationResult
class TripCreationSuccess : TripCreationResult()
class TripCreationError(val exception: Exception) : TripCreationResult()