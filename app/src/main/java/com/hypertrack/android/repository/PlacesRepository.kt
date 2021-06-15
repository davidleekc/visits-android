package com.hypertrack.android.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fonfon.kgeohash.GeoHash
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.ApiClient
import com.hypertrack.android.api.Geofence
import com.hypertrack.android.api.GeofenceProperties
import com.hypertrack.android.api.GeofenceResponse
import com.hypertrack.android.models.GeofenceMetadata
import com.hypertrack.android.models.Integration
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.common.nullIfEmpty
import com.hypertrack.android.utils.OsUtilsProvider
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import retrofit2.HttpException

interface PlacesRepository {
    suspend fun loadPage(pageToken: String?, gh: GeoHash? = null): GeofencesPage
    suspend fun createGeofence(
        latitude: Double,
        longitude: Double,
        name: String? = null,
        address: String? = null,
        description: String? = null,
        integration: Integration? = null
    ): CreateGeofenceResult
}

class PlacesRepositoryImpl(
    private val apiClient: ApiClient,
    private val moshi: Moshi,
    private val osUtilsProvider: OsUtilsProvider
) : PlacesRepository {

    override suspend fun loadPage(pageToken: String?, gh: GeoHash?): GeofencesPage {
        //todo task geohash
        val res = apiClient.getGeofences(pageToken, gh.toString())
        val localGeofences =
            res.geofences.map { LocalGeofence.fromGeofence(it, moshi, osUtilsProvider) }
        return GeofencesPage(
            localGeofences,
            res.paginationToken
        )
    }

    override suspend fun createGeofence(
        latitude: Double,
        longitude: Double,
        name: String?,
        address: String?,
        description: String?,
        integration: Integration?
    ): CreateGeofenceResult {
        try {
            val res = apiClient.createGeofence(
                latitude, longitude, GeofenceMetadata(
                    name = name.nullIfEmpty() ?: integration?.name,
                    integration = integration,
                    description = description.nullIfEmpty(),
                    address = address.nullIfEmpty()
                )
            )
            if (res.isSuccessful) {
                return CreateGeofenceSuccess(
                    LocalGeofence.fromGeofence(
                        res.body()!!.first(),
                        moshi,
                        osUtilsProvider
                    )
                )
            } else {
                return CreateGeofenceError(HttpException(res))
            }
        } catch (e: Exception) {
            return CreateGeofenceError(e)
        }
    }

}

class GeofencesPage(
    val geofences: List<LocalGeofence>,
    val paginationToken: String?
)

sealed class CreateGeofenceResult
class CreateGeofenceSuccess(val geofence: LocalGeofence) : CreateGeofenceResult()
class CreateGeofenceError(val e: Exception) : CreateGeofenceResult()