package com.hypertrack.android.models.local

import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.Geofence
import com.hypertrack.android.api.GeofenceMarker
import com.hypertrack.android.models.*
import com.hypertrack.android.ui.common.nullIfEmpty
import com.hypertrack.android.ui.common.toAddressString
import com.hypertrack.android.ui.common.toShortAddressString
import com.hypertrack.android.utils.Meter
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.android.parcel.Parcelize
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
data class LocalGeofence(
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
            //all parsed metadata fields should be removed to avoid duplication
            val metadata = geofence.metadata?.toMutableMap() ?: mutableMapOf()

            val metadataAddress = metadata.remove(GeofenceMetadata.KEY_ADDRESS) as String?
            val address = geofence.address.nullIfEmpty()
                ?: metadataAddress.nullIfEmpty()
            val integration = metadata.remove(GeofenceMetadata.KEY_INTEGRATION)?.let {
                try {
                    moshi.adapter(GeofenceMetadata::class.java)
                        .fromJsonValue(metadata)?.integration
                } catch (_: Exception) {
                    null
                }
            }

            return LocalGeofence(
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