package com.hypertrack.android.models.local

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.Geofence
import com.hypertrack.android.api.GeofenceMarker
import com.hypertrack.android.models.*
import com.hypertrack.android.ui.common.nullIfEmpty
import com.hypertrack.android.ui.common.toAddressString
import com.hypertrack.android.ui.common.toShortAddressString
import com.hypertrack.android.utils.OsUtilsProvider
import com.squareup.moshi.Moshi
import java.time.ZonedDateTime

class LocalGeofence(
    private val geofence: Geofence,
    val name: String?,
    val integration: Integration?,
    val shortAddress: String?,
    val fullAddress: String?,
    val metadata: Map<String, String>
) {

    val id = geofence._id

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

    val markers =
        geofence.marker?.markers?.sortedByDescending { it.arrival!!.recordedAt } ?: listOf()

    val visitsCount: Int by lazy {
        markers.count()
    }

    val lastVisit: GeofenceMarker? by lazy {
        markers.firstOrNull()
    }

    val radius = geofence.radius

    val createdAt: ZonedDateTime = ZonedDateTime.parse(geofence.created_at)

    companion object {
        fun fromGeofence(
            geofence: Geofence,
            moshi: Moshi,
            osUtilsProvider: OsUtilsProvider
        ): LocalGeofence {
            val metadata = geofence.metadata?.toMutableMap() ?: mutableMapOf()
            val metadataAddress = metadata.remove(GeofenceMetadata.KEY_ADDRESS) as String?

            return LocalGeofence(
                geofence = geofence,
                shortAddress = geofence.address?.let { "${it.street}" }
                    ?: metadataAddress.nullIfEmpty()
                    ?: osUtilsProvider.getPlaceFromCoordinates(
                        latitude = geofence.latitude,
                        longitude = geofence.longitude
                    )?.toShortAddressString(),
                fullAddress = geofence.address?.let { "${it.city}, ${it.street}" }
                    ?: metadataAddress.nullIfEmpty()
                    ?: osUtilsProvider.getPlaceFromCoordinates(
                        latitude = geofence.latitude,
                        longitude = geofence.longitude
                    )?.toAddressString(),
                name = metadata.remove(GeofenceMetadata.KEY_NAME) as String?,
                integration = metadata.remove(GeofenceMetadata.KEY_INTEGRATION)?.let {
                    try {
                        moshi.adapter(GeofenceMetadata::class.java)
                            .fromJsonValue(metadata)?.integration
                    } catch (_: Exception) {
                        null
                    }
                },
                metadata = metadata.filter { it.value is String } as Map<String, String>
            )
        }
    }
}