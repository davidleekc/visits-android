package com.hypertrack.android.models.local

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.*
import com.hypertrack.android.models.GeofenceMetadata
import java.time.LocalDate
import java.time.ZonedDateTime

class LocalGeofenceVisit(
    val id: String,
    val geofenceId: String,
    val deviceId: String,
    val arrival: ZonedDateTime,
    val exit: ZonedDateTime?,
    val location: LatLng,
    val routeTo: RouteTo?,
    val durationSeconds: Int?,
    val address: String?,
    val metadata: GeofenceMetadata?
) {
    fun getDay(): LocalDate {
        return if (exit != null) {
            exit.toLocalDate()
        } else {
            arrival.toLocalDate()
        }
    }

    companion object {
        fun fromVisit(visit: GeofenceVisit): LocalGeofenceVisit {
            return LocalGeofenceVisit(
                id = visit.markerId!!,
                geofenceId = visit.geofenceId,
                deviceId = visit.deviceId,
                arrival = ZonedDateTime.parse(visit.arrival!!.recordedAt),
                exit = ZonedDateTime.parse(visit.exit?.recordedAt),
                location = visit.geometry!!.let { LatLng(it.latitude, it.longitude) },
                routeTo = visit.routeTo,
                durationSeconds = visit.duration,
                address = visit.address,
                metadata = visit.metadata,
            )
        }
    }
}