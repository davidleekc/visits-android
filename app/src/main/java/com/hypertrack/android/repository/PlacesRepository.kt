package com.hypertrack.android.repository

import androidx.lifecycle.MutableLiveData
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
    fun refresh()
    suspend fun loadPage(pageToken: String?): GeofencesPage
    fun getGeofence(geofenceId: String): LocalGeofence
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
    private val integrationsRepository: IntegrationsRepository,
    private val moshi: Moshi,
    private val osUtilsProvider: OsUtilsProvider
) : PlacesRepository {

    private val geofencesCache = mutableMapOf<String, Geofence>()

    override fun refresh() {
        integrationsRepository.invalidateCache()
    }

    override suspend fun loadPage(pageToken: String?): GeofencesPage {
        val res = apiClient.getGeofences(pageToken)
        res.geofences.forEach { geofencesCache.put(it.geofence_id, it) }
        return GeofencesPage(
            res.geofences.map { LocalGeofence.fromGeofence(it, moshi, osUtilsProvider) },
            res.paginationToken
        )
    }

    override fun getGeofence(geofenceId: String): LocalGeofence {
        return LocalGeofence.fromGeofence(
            geofencesCache.getValue(geofenceId),
            moshi,
            osUtilsProvider
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
                return CreateGeofenceSuccess
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
object CreateGeofenceSuccess : CreateGeofenceResult()
class CreateGeofenceError(val e: Exception) : CreateGeofenceResult()