package com.hypertrack.android.ui.common

import android.graphics.Color
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HypertrackMapWrapper(
    val googleMap: GoogleMap,
    private val osUtilsProvider: OsUtilsProvider
) {

    val geofenceMarkerIcon by lazy {
        BitmapDescriptorFactory.fromBitmap(
            osUtilsProvider.bitmapFormResource(
                R.drawable.ic_ht_departure_active
            )
        )
    }

    fun addGeofenceCircle(geofence: LocalGeofence) {
        if (geofence.isPolygon) {
            googleMap.addPolygon(
                PolygonOptions()
                    .addAll(geofence.polygon!!)
                    .fillColor(osUtilsProvider.colorFromResource(R.color.colorGeofenceFill))
                    .strokeColor(osUtilsProvider.colorFromResource(R.color.colorGeofence))
                    .strokeWidth(3f)
                    .visible(true)
            )
        } else {
            geofence.radius?.let { radius ->
                googleMap.addCircle(
                    CircleOptions()
                        .center(geofence.latLng)
                        .fillColor(osUtilsProvider.colorFromResource(R.color.colorGeofenceFill))
                        .strokeColor(osUtilsProvider.colorFromResource(R.color.colorGeofence))
                        .strokeWidth(3f)
                        .radius(radius.toDouble())
                        .visible(true)
                )
            }
            googleMap.addCircle(
                CircleOptions()
                    .center(geofence.latLng)
                    .fillColor(osUtilsProvider.colorFromResource(R.color.colorGeofence))
                    .strokeColor(Color.TRANSPARENT)
                    .radius(30.0)
                    .visible(true)
            )
        }
    }

    fun addGeofenceMarker(geofence: LocalGeofence) {
        val it = geofence
        if (geofence.isPolygon) {
            googleMap.addPolygon(
                PolygonOptions()
                    .addAll(geofence.polygon!!)
                    .fillColor(osUtilsProvider.colorFromResource(R.color.colorGeofenceFill))
                    .strokeColor(osUtilsProvider.colorFromResource(R.color.colorGeofence))
                    .strokeWidth(3f)
                    .visible(true)
            )
        } else {
            it.radius?.let { radius ->
                googleMap.addCircle(
                    CircleOptions()
                        .radius(radius.toDouble())
                        .center(it.latLng)
                        .fillColor(osUtilsProvider.colorFromResource(R.color.colorGeofenceFill))
                        .strokeColor(
                            osUtilsProvider.colorFromResource(
                                R.color.colorHyperTrackGreenSemitransparent
                            )
                        )
                )
            }
        }

        googleMap.addMarker(
            MarkerOptions()
                .icon(geofenceMarkerIcon)
                .snippet(it.id)
                .position(it.latLng)
                .anchor(0.5f, 0.5f)
        )
    }

    fun moveCamera(latLng: LatLng) {
        GlobalScope.launch(Dispatchers.Main) {
            googleMap.moveCamera(latLng)
        }
    }

    override fun toString(): String {
        return javaClass.simpleName
    }

    companion object {
        const val DEFAULT_ZOOM = 13f
    }

}

fun GoogleMap.moveCamera(latLng: LatLng) {
    moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, HypertrackMapWrapper.DEFAULT_ZOOM))
}

val GoogleMap.viewportPosition: LatLng
    get() {
        return cameraPosition.target
    }
