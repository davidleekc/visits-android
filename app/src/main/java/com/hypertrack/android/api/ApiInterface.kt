package com.hypertrack.android.api

import android.graphics.Bitmap
import com.hypertrack.android.models.Address
import com.hypertrack.android.models.VisitDataSource
import com.hypertrack.android.models.VisitType
import com.hypertrack.android.toBase64
import com.hypertrack.android.toNote
import com.hypertrack.logistics.android.github.R
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.*

interface ApiInterface {

    @POST("client/devices/{device_id}/start")
    suspend fun clockIn(@Path("device_id") deviceId: String)

    @POST("client/devices/{device_id}/stop")
    suspend fun clockOut(@Path("device_id")deviceId : String)

    @POST("client/devices/{device_id}/image")
    suspend fun persistImage(
        @Path("device_id")deviceId : String,
        @Body encodedImage: EncodedImage
    ) : Response<ImageResponse>

    @GET("client/geofences?include_archived=false&include_markers=true")
    suspend fun getGeofences(
        @Query("device_id")deviceId : String,
        @Query("pagination_token")paginationToken: String
    ) : Response<GeofenceResponse>

    @GET("client/geofences/markers")
    suspend fun getGeofenceMarkers(
        @Query("device_id")deviceId : String,
        @Query("pagination_token")paginationToken: String
    ) : Response<GeofenceMarkersResponse>

    @GET("client/trips")
    suspend fun getTrips(
        @Query("device_id")deviceId : String,
        @Query("pagination_token")paginationToken: String
    ) : Response<TripResponse>

    /**
     * client/devices/A24BA1B4-1234-36F7-8DD7-15D97C3FD912/history/2021-02-05?timezone=Europe%2FZaporozhye
     */
    @GET("client/devices/{device_id}/history/{day}")
    suspend fun getHistory(
        @Path("device_id") deviceId: String,
        @Path("day") day: String,
        @Query("timezone") timezone: String
    ) : Response<HistoryResponse>

}

@JsonClass(generateAdapter = true)
data class EncodedImage(@field:Json(name = "data") val data: String) {
    constructor(bitmap: Bitmap) : this(bitmap.toBase64())
}

@JsonClass(generateAdapter = true)
data class TripResponse(
    @field:Json(name = "data") val trips: List<Trip>,
    @field:Json(name = "pagination_token") val paginationToken: String?
)

@JsonClass(generateAdapter = true)
data class GeofenceMarkersResponse(
    @field:Json(name = "data") val markers: List<GeofenceMarker>,
    @field:Json(name = "pagination_token") val next: String?
)

@JsonClass(generateAdapter = true)
data class GeofenceResponse(
    @field:Json(name = "data") val geofences: List<Geofence>,
    @field:Json(name = "pagination_token") val paginationToken: String?
)

@JsonClass(generateAdapter = true)
data class ImageResponse(
    @field:Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class Trip(
    @field:Json(name = "views") val views: Views,
    @field:Json(name = "trip_id") val tripId: String,
    @field:Json(name = "started_at") override val createdAt: String,
    @field:Json(name = "metadata") val metadata : Map<String, Any>?,
    @field:Json(name = "destination") val destination: TripDestination?
): VisitDataSource {
    override val visitedAt: String
        get() = destination?.arrivedAt?:""
    override val _id: String
        get() = tripId
    override val customerNote: String
        get() = metadata.toNote()
    override val latitude: Double
        get() = destination?.geometry?.latitude ?: 0.0
    override val longitude: Double
        get() = destination?.geometry?.longitude ?: 0.0
    override val address: Address?
        get() = destination?.address?.let { Address(it, "", "", "") }
    override val visitType: VisitType
        get() = VisitType.TRIP
    override val visitNamePrefixId: Int
        get() = R.string.trip_to
    override val visitNameSuffix: String
        get() = if (destination?.address == null) " [$longitude, $latitude]" else " ${destination.address}"
}

@JsonClass(generateAdapter = true)
data class TripDestination(
    @field:Json(name = "address") val address: String?,
    @field:Json(name = "geometry") val geometry: Geometry,
    @field:Json(name = "arrived_at") val arrivedAt: String?
)

@JsonClass(generateAdapter = true)
data class Views(
    @field:Json(name = "share_url") val shareUrl: String?,
    @field:Json(name = "embed_url") val embedUrl: String?
)

@JsonClass(generateAdapter = true)
data class Geofence(
    @field:Json(name = "geofence_id") val geofence_id : String,
    @field:Json(name = "created_at") val created_at : String,
    @field:Json(name = "metadata") val metadata : Map<String, Any>?,
    @field:Json(name = "geometry") val geometry : Geometry,
    @field:Json(name = "markers") val marker: GeofenceMarkersResponse?,
    @field:Json(name = "radius") val radius : Int?
): VisitDataSource {
    override val latitude: Double
        get() = geometry.latitude
    override val longitude: Double
        get() = geometry.longitude
    override val _id: String
        get() = geofence_id
    override val customerNote: String
        get() = metadata.toNote()
    override val address: Address?
        get() = null
    override val createdAt: String
        get() = created_at
    override val visitedAt: String
        get() = marker?.markers?.first()?.arrival?.recordedAt ?: ""
    override val visitType
        get() = VisitType.GEOFENCE
    override val visitNamePrefixId: Int
        get() = R.string.geofence_at
    override val visitNameSuffix: String
        get() = if (address == null) "[$longitude, $latitude]" else "$address"
    val type: String
        get() = geometry.type
}

class Point (
    @field:Json(name = "coordinates") override val coordinates : List<Double>
) : Geometry() {
    override val type: String
        get() = "Point"

    override val latitude: Double
        get() = coordinates[1]

    override val longitude: Double
        get() = coordinates[0]
}

class Polygon (
    @field:Json(name = "coordinates") override val coordinates : List<List<List<Double>>>
) : Geometry() {
    override val type: String
            get() = "Polygon"
    override val latitude: Double
        get() = coordinates[0].map { it[1] }.average()
    override val longitude: Double
        get() = coordinates[0].map { it[0] }.average()
}

abstract class Geometry {
    abstract val coordinates: List<*>
    abstract val type: String
    abstract val latitude: Double
    abstract val longitude: Double
}

@JsonClass(generateAdapter = true)
data class GeofenceMarker(
    @field:Json(name = "geofence_id") val geofenceId: String,
    @field:Json(name = "arrival") val arrival: Arrival?
)
@JsonClass(generateAdapter = true)
data class Arrival(@field:Json(name = "recorded_at") val recordedAt: String = "")

@JsonClass(generateAdapter = true)
data class HistoryResponse(
    @field:Json(name = "locations") val locations: Locations,
    @field:Json(name = "markers") val markers: List<HistoryMarker>,
    @field:Json(name = "active_duration") val activeDuration: Int,
    @field:Json(name = "distance") val distance: Int,
    @field:Json(name = "drive_duration") val driveDuration: Int?,
    @field:Json(name = "duration") val duration: Int,
    @field:Json(name = "geofences_visited") val geofencesCount: Int,
    @field:Json(name = "geotags") val geotagsCount: Int,
    @field:Json(name = "inactive_duration") val inactiveDuration: Int,
    // Skip reasons for now as constants aren't deserialized automatically but their usage is questionable
//    @field:Json(name = "inactive_reasons") val inactiveReasons: List<Any>,
    @field:Json(name = "stop_duration") val stopDuration: Int,
    @field:Json(name = "tracking_rate") val trackingRate: Int,
    @field:Json(name = "trips") val tripsCount: Int,
    @field:Json(name = "walk_duration") val walkDuration: Int
)

@JsonClass(generateAdapter = true)
data class Insights(
    @field:Json(name = "estimated_distance") val estimatedDistance: Int,
    @field:Json(name = "geofences_idle_time") val geofencesIdleTime: Int,
    @field:Json(name = "geofences_route_to_time") val geofencesRouteToTime: Int,
    @field:Json(name = "geofences_time") val geofencesTime: Int,
    @field:Json(name = "geotags_route_to_time") val geotagsRouteToTime: Int,
    @field:Json(name = "step_count") val stepCount: Int,
    @field:Json(name = "total_tracking_time") val totalTrackingTime: Int,
    @field:Json(name = "trips_arrived_at_destination") val tripsArrivedAtDestination: Int,
    @field:Json(name = "trips_on_time") val tripsOnTime: Int,
)

interface HistoryMarker {
    val markerId: String
    val type: String
    val data: Any
}

@JsonClass(generateAdapter = true)
data class HistoryStatusMarker(
    @field:Json(name = "marker_id") override val markerId: String,
    @field:Json(name = "type") override val type: String = "device_status",
    @field:Json(name = "data")  override val data: HistoryStatusMarkerData
) : HistoryMarker

@JsonClass(generateAdapter = true)
data class HistoryStatusMarkerData(
    @field:Json(name = "value") val value: String,
    @field:Json(name = "duration") val duration: Int,
    @field:Json(name = "start") val start: MarkerTerminal,
    @field:Json(name = "end") val end: MarkerTerminal,
)

// Do not be misguided by name. It's a geotag.
@JsonClass(generateAdapter = true)
data class HistoryTripMarker(
    @field:Json(name = "marker_id") override val markerId: String,
    @field:Json(name = "type") override val type: String = "trip_marker",
    @field:Json(name = "data") override val data: HistoryTripMarkerData,
) : HistoryMarker

@JsonClass(generateAdapter = true)
data class HistoryTripMarkerData(
    @field:Json(name = "recorded_at") val recordedAt: String,
    @field:Json(name = "metadata") val metadata: Map<String, Any>?,
    @field:Json(name = "location") val location: HistoryTripMarkerLocation?,
    @field:Json(name = "route_to") val routeTo: MarkerRoute?,
)
@JsonClass(generateAdapter = true)
data class HistoryTripMarkerLocation(val coordinates: List<Double>)

@JsonClass(generateAdapter = true)
data class HistoryGeofenceMarker(
    @field:Json(name = "marker_id") override val markerId: String,
    @field:Json(name = "type") override val type: String = "geofence",
    @field:Json(name = "data") override val data: HistoryGeofenceMarkerData,
) : HistoryMarker

@JsonClass(generateAdapter = true)
data class HistoryGeofenceMarkerData(
    @field:Json(name = "duration") val duration: Int,
    @field:Json(name = "arrival") val arrival: HistoryGeofenceMarkerArrival,
    @field:Json(name = "geofence") val geofence: HistoryGeofenceMarkerGeofence,
)

@JsonClass(generateAdapter = true)
data class HistoryGeofenceMarkerGeofence(@field:Json(name = "geofence_id") val geofenceId: String)

@JsonClass(generateAdapter = true)
data class HistoryGeofenceMarkerArrival(@field:Json(name = "location") val location: HistoryGeofenceArrivalLocation)
@JsonClass(generateAdapter = true)
data class HistoryGeofenceArrivalLocation(
    @field:Json(name="geometry") val geometry: Geometry,
    @field:Json(name = "recorded_at") val recordedAt: String,
)

@JsonClass(generateAdapter = true)
data class MarkerRoute(
    @field:Json(name = "distance") val distance: Int,
    @field:Json(name = "duration") val duration: Int,
)

@JsonClass(generateAdapter = true)
data class MarkerTerminal(
    @field:Json(name = "location") val location: MarkerLocation?,
    @field:Json(name = "recorded_at") val recordedAt: String,
)

@JsonClass(generateAdapter = true)
data class MarkerLocation(@field:Json(name = "geometry") val geometry: Geometry)

@JsonClass(generateAdapter = true)
data class Locations(
    @field:Json(name = "coordinates") val coordinates: List<HistoryCoordinate>,
    @field:Json(name = "type") val type: String
)

class HistoryCoordinate(
    val longitude: Double,
    val latitude: Double,
    val altitude: Double?,
    val timestamp: String,
)
