package com.hypertrack.android.repository

import com.fonfon.kgeohash.GeoHash
import com.hypertrack.android.api.ApiClient
import com.hypertrack.android.api.GeofenceVisit
import com.hypertrack.android.models.GeofenceMetadata
import com.hypertrack.android.models.Integration
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.ui.common.DataPage
import com.hypertrack.android.ui.common.nullIfBlank
import com.hypertrack.android.utils.OsUtilsProvider
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

interface PlacesRepository {
    //todo use DataPage
    suspend fun loadGeofencesPage(pageToken: String?, gh: GeoHash? = null): GeofencesPage
    suspend fun loadAllGeofencesVisitsPage(pageToken: String?): DataPage<GeofenceVisit>
    suspend fun createGeofence(
        latitude: Double,
        longitude: Double,
        radius: Int,
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

    override suspend fun loadGeofencesPage(pageToken: String?, gh: GeoHash?): GeofencesPage {
        return withContext(Dispatchers.IO) {
            val res = apiClient.getGeofences(pageToken, gh.string())
            val localGeofences =
                res.geofences.map { LocalGeofence.fromGeofence(it, moshi, osUtilsProvider) }
            GeofencesPage(
                localGeofences,
                res.paginationToken
            )
        }
    }

    override suspend fun loadAllGeofencesVisitsPage(
        pageToken: String?,
    ): DataPage<GeofenceVisit> {
        return apiClient.getAllGeofencesVisits(pageToken).let {
            DataPage(it.visits, it.paginationToken)
        }
    }

    override suspend fun createGeofence(
        latitude: Double,
        longitude: Double,
        radius: Int,
        name: String?,
        address: String?,
        description: String?,
        integration: Integration?
    ): CreateGeofenceResult {
        try {
            val res = apiClient.createGeofence(
                latitude = latitude,
                longitude = longitude,
                radius = radius,
                GeofenceMetadata(
                    name = name.nullIfBlank() ?: integration?.name,
                    integration = integration,
                    description = description.nullIfBlank(),
                    address = address.nullIfBlank()
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

fun GeoHash?.string() = this?.let { it.toString() }

class GeofencesPage(
    val geofences: List<LocalGeofence>,
    val paginationToken: String?
)

sealed class CreateGeofenceResult
class CreateGeofenceSuccess(val geofence: LocalGeofence) : CreateGeofenceResult()
class CreateGeofenceError(val e: Exception) : CreateGeofenceResult()