package com.hypertrack.android.ui.common

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.utils.Intersect
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class HypertrackMapWrapper(
    val googleMap: GoogleMap,
    private val osUtilsProvider: OsUtilsProvider,
    private val params: MapParams
) {


    init {
        googleMap.uiSettings.apply {
            isScrollGesturesEnabled = params.enableScroll
            isZoomControlsEnabled = params.enableZoomKeys
            isMyLocationButtonEnabled = params.enableMyLocationButton
        }

        try {
            @SuppressLint("MissingPermission")
            googleMap.isMyLocationEnabled = params.enableMyLocationIndicator
        } catch (_: Exception) {
        }
    }

    val cameraPosition: LatLng
        get() = googleMap.viewportPosition

    val geofenceMarkerIcon: BitmapDescriptor by lazy {
        BitmapDescriptorFactory.fromBitmap(
            osUtilsProvider.bitmapFormResource(
                R.drawable.ic_ht_departure_active
            )
        )
    }

    fun setOnCameraMovedListener(listener: (LatLng) -> Unit) {
        googleMap.setOnCameraIdleListener { listener.invoke(googleMap.viewportPosition) }
    }

    fun addGeofenceShape(geofence: LocalGeofence) {
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

    fun addGeofenceShape(latLng: LatLng, radius: Int): List<Circle> {
        val res = mutableListOf<Circle>()
        googleMap.addCircle(
            CircleOptions()
                .center(latLng)
                .fillColor(osUtilsProvider.colorFromResource(R.color.colorGeofenceFill))
                .strokeColor(osUtilsProvider.colorFromResource(R.color.colorGeofence))
                .strokeWidth(3f)
                .radius(radius.toDouble())
                .visible(true)
        ).also {
            res.add(it)
        }
        googleMap.addCircle(
            CircleOptions()
                .center(latLng)
                .fillColor(osUtilsProvider.colorFromResource(R.color.colorGeofence))
                .strokeColor(Color.TRANSPARENT)
                .radius(15.0)
                .visible(true)
        ).also {
            res.add(it)
        }
        return res
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

    fun addNewGeofenceRadius(latLng: LatLng, radius: Int): Circle {
        return googleMap.addCircle(
            CircleOptions()
                .radius(radius.toDouble())
                .center(latLng)
                .strokePattern(listOf(Dash(30f), Gap(20f)))
                .fillColor(osUtilsProvider.colorFromResource(R.color.colorGeofenceFill))
                .strokeColor(
                    osUtilsProvider.colorFromResource(
                        R.color.colorHyperTrackGreenSemitransparent
                    )
                )
        )
    }

    fun moveCamera(latLng: LatLng, zoom: Float? = null) {
        GlobalScope.launch(Dispatchers.Main) {
            googleMap.moveCamera(latLng, zoom)
        }
    }

    fun setOnMapClickListener(listener: () -> Unit) {
        googleMap.setOnMapClickListener { listener.invoke() }
    }

    override fun toString(): String {
        return javaClass.simpleName
    }

    companion object {
        const val DEFAULT_ZOOM = 13f
    }

}

class MapParams(
    val enableScroll: Boolean,
    val enableZoomKeys: Boolean,
    val enableMyLocationButton: Boolean,
    val enableMyLocationIndicator: Boolean
)

fun GoogleMap.moveCamera(latLng: LatLng, zoom: Float?) {
    moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom ?: HypertrackMapWrapper.DEFAULT_ZOOM))
}

val GoogleMap.viewportPosition: LatLng
    get() {
        return cameraPosition.target
    }
