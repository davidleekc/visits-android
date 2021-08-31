package com.hypertrack.android.models.local

import android.annotation.SuppressLint
import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.Geofence
import com.hypertrack.android.api.GeofenceVisit
import com.hypertrack.android.api.Polygon
import com.hypertrack.android.models.*
import com.hypertrack.android.ui.common.nullIfBlank
import com.hypertrack.android.utils.OsUtilsProvider
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
data class LocalGeofence(
    val currentDeviceId: String,
    val geofence: Geofence,
    val name: String?,
    val address: String?,
    val integration: Integration?,
    val metadata: Map<String, String>
) {

    val id = geofence.geofence_id

    val latitude: Double
        get() = geofence.geometry.latitude

    val longitude: Double
        get() = geofence.geometry.longitude

    val latLng: LatLng
        get() = LatLng(latitude, longitude)

    val location: Location
        get() = Location(
            latitude = latitude,
            longitude = longitude
        )

    val isPolygon: Boolean = geofence.geometry is Polygon

    val polygon: List<LatLng>? = if (geofence.geometry is Polygon) {
        geofence.geometry.coordinates.first().map {
            LatLng(it[1], it[0])
        }
    } else null

    val markers =
        geofence.marker?.visits
            ?.filter { it.deviceId == currentDeviceId }
            ?.sortedByDescending { it.arrival!!.recordedAt }
            ?: listOf()

    val visitsCount: Int by lazy {
        markers.count()
    }

    val lastVisit: GeofenceVisit? by lazy {
        markers.firstOrNull()
    }

    val radius = geofence.radius

    val createdAt: ZonedDateTime = ZonedDateTime.parse(geofence.created_at)

    companion object {
        fun fromGeofence(
            currentDeviceId: String,
            geofence: Geofence,
            moshi: Moshi,
            osUtilsProvider: OsUtilsProvider
        ): LocalGeofence {
            //all parsed metadata fields should be removed to avoid duplication
            val metadata = geofence.metadata?.toMutableMap() ?: mutableMapOf()

            val metadataAddress = metadata.remove(GeofenceMetadata.KEY_ADDRESS) as String?
            val address = geofence.address.nullIfBlank()
                ?: metadataAddress.nullIfBlank()
            val integration = metadata.remove(GeofenceMetadata.KEY_INTEGRATION)?.let {
                try {
                    moshi.adapter(GeofenceMetadata::class.java)
                        .fromJsonValue(metadata)?.integration
                } catch (_: Exception) {
                    null
                }
            }

            return LocalGeofence(
                currentDeviceId = currentDeviceId,
                geofence = geofence,
                name = metadata.remove(GeofenceMetadata.KEY_NAME) as String?,
                address = address,
                integration = integration,
                metadata = metadata.filter { it.value is String } as Map<String, String>
            )
        }
    }
}

@Parcelize
class LocalGeofenceJson(private val jsonString: String) : Parcelable {
    constructor(moshi: Moshi, localGeofence: LocalGeofence) : this(
        moshi.adapter(LocalGeofence::class.java).toJson(localGeofence)
    )

    fun getValue(moshi: Moshi): LocalGeofence {
        return moshi.adapter(LocalGeofence::class.java).fromJson(jsonString)!!
    }
}