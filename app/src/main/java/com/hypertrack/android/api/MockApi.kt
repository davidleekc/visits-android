package com.hypertrack.android.api

import android.util.Log
import com.fonfon.kgeohash.GeoHash
import com.hypertrack.android.utils.Injector
import com.hypertrack.android.utils.MockData
import com.hypertrack.android.utils.MyApplication
import kotlinx.coroutines.delay
import retrofit2.Response
import retrofit2.http.Query
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Suppress("BlockingMethodInNonBlockingContext")
class MockApi(val remoteApi: ApiInterface) : ApiInterface by remoteApi {

    private val fences = mutableListOf<Geofence>()

    override suspend fun createGeofences(
        deviceId: String,
        params: GeofenceParams
    ): Response<List<Geofence>> {
        return remoteApi.createGeofences(deviceId, params)
//        fences.add(
//            Geofence(
//                "",
//                "00000000-0000-0000-0000-000000000000",
//                "",
//                params.geofences.first().metadata,
//                params.geofences.first().geometry,
//                null,
//                100,
//                false,
//            )
//        )
//        return Response.success(listOf())
    }

    override suspend fun getDeviceGeofences(
        deviceId: String,
        geohash: String?,
        paginationToken: String?,
        includeArchived: Boolean,
        sortNearest: Boolean
    ): Response<GeofenceResponse> {
//        return Response.success(
//            Injector.getMoshi().adapter(GeofenceResponse::class.java)
//                .fromJson(MockData.MOCK_GEOFENCES_JSON)
//        )

        delay((500 + Math.random() * 1000).toLong())

        val page = (paginationToken?.split("_")?.get(0)?.toInt() ?: 0) + 1
        val totalPages =
            (paginationToken?.split("_")?.get(1)?.toInt()) ?: (3 + (Math.random() * 3f).toInt())

        val gh = geohash?.let { GeoHash(it) }

        return Response.success(
            GeofenceResponse(
                (0..10).map {
                    generateGeofence(
                        0,
                        lat = gh?.boundingBox?.let { Math.random() * (it.maxLat - it.minLat) },
                        lon = gh?.boundingBox?.let { Math.random() * (it.maxLon - it.minLon) }
                    )
                },
                if (page <= totalPages) {
                    "${page}_${totalPages}"
                } else {
                    null
                }
            )
        )
    }

    override suspend fun getGeofencesWithMarkers(
        paginationToken: String?,
        deviceId: String,
        includeArchived: Boolean,
        sortNearest: Boolean
    ): Response<GeofenceResponse> {
//        return Response.success(GeofenceResponse(fences, null))

        return Response.success(
            Injector.getMoshi().adapter(GeofenceResponse::class.java)
                .fromJson(MockData.MOCK_GEOFENCES_JSON)
        )

//        val page = try {
//            paginationToken?.toInt() ?: 0
//        } catch (_: Exception) {
//            0
//        }
//        Log.v("hypertrack-verbose", page.toString())
//        return Response.success(
//            GeofenceResponse(
//                (0..10).map {
//                    generateGeofence(page), if (page < 5) {
//                    (page + 1).toString()
//                } else {
//                    null
//                }
//            )
//        )
    }

    override suspend fun getIntegrations(
        query: String?,
        limit: Int?
    ): Response<IntegrationsResponse> {
        return Response.success(
            Injector.getMoshi().adapter(IntegrationsResponse::class.java)
                .fromJson(MockData.MOCK_INTEGRATIONS_RESPONSE)!!.let {
                    if (query != null) {
                        it.copy(data = it.data.filter { it.name?.contains(query.toString()) == true })
                    } else {
                        it
                    }
                }
        )
    }

    private fun generateGeofence(
        page: Int,
        lat: Double? = 122.395223,
        lon: Double? = 37.794763
    ): Geofence {
        return Geofence(
            "",
            "",
            ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT),
            mapOf("name" to page.toString()),
            """
                {
                "type":"Point",
                "coordinates": [${lat ?: 122.395223}, ${lon ?: 37.794763}]
            }
            """.let {
                Injector.getMoshi().adapter(Geometry::class.java).fromJson(it)!!
            },
            null,
            100,
            false,
        )
    }
}