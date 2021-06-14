package com.hypertrack.android.ui.screens.add_place

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.location.Location
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.net.PlacesClient
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.interactors.PlacesInteractorImpl
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.screens.select_destination.SelectDestinationViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.history.DeviceLocationProvider
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.BuildConfig
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.launch


class AddPlaceViewModel(
    private val osUtilsProvider: OsUtilsProvider,
    private val placesClient: PlacesClient,
    private val deviceLocationProvider: DeviceLocationProvider,
    private val placesInteractor: PlacesInteractor
) : SelectDestinationViewModel(
    osUtilsProvider,
    placesClient,
    deviceLocationProvider
) {

    override val loadingStateBase = placesInteractor.isLoadingForLocation

    override val errorBase = MediatorLiveData<Consumable<String>>().apply {
        addSource(placesInteractor.errorFlow.asLiveData()) {
            postValue(it.map {
                osUtilsProvider.getErrorMessage(it)
            })
        }
    }

    init {
        MediatorLiveData<Pair<GoogleMap?, List<LocalGeofence>?>>().apply {
            addSource(map) {
                postValue(Pair(map.value, placesInteractor.geofences.value?.values?.toList()))
            }
            addSource(placesInteractor.geofences) {
                postValue(Pair(map.value, placesInteractor.geofences.value?.values?.toList()))
            }
        }.observeForever { p ->
            p.first?.let { p.second?.let { onDisplayGeofencesOnMap(p.first!!, p.second!!) } }
        }
    }

    private val icon = BitmapDescriptorFactory.fromBitmap(
        osUtilsProvider.bitmapFormResource(
            R.drawable.ic_ht_departure_active
        )
    )

    protected override fun proceed(latLng: LatLng, address: String?) {
        destination.postValue(
            AddPlaceFragmentDirections.actionAddPlaceFragmentToAddPlaceInfoFragment(
                latLng,
                address = address,
                name = currentPlace?.name
            )
        )
    }

    override fun onMapReady(googleMap: GoogleMap) {
        super.onMapReady(googleMap)
        googleMap.setOnMarkerClickListener {
            it.snippet?.let { snippet ->
                destination.postValue(
                    AddPlaceFragmentDirections.actionAddPlaceFragmentToPlaceDetailsFragment(
                        snippet
                    )
                )
                return@setOnMarkerClickListener true
            }
            false
        }
    }

    override fun onCameraMoved(map: GoogleMap) {
        val region = map.projection.visibleRegion
        placesInteractor.loadGeofencesForMap(map.cameraPosition.target, region)
    }

    private fun onDisplayGeofencesOnMap(googleMap: GoogleMap, geofences: List<LocalGeofence>) {
        viewModelScope.launch {
            googleMap.clear()


            val placesInteractor = placesInteractor as PlacesInteractorImpl

            if (BuildConfig.DEBUG) {
                placesInteractor.geoCache.getItems().forEach {
                    googleMap.addMarker(
                        MarkerOptions().anchor(0.5f, 0.5f).position(
                            it.first.toLocation().toLatLng()
                        )
                            .icon(createPureTextIcon("${it.second.isLoading()}"))
                    )

                    it.first.boundingBox
                        .let { bb ->
                            listOf(
                                bb.bottomLeft,
                                bb.bottomRight,
                                bb.topRight,
                                bb.topLeft,
                                bb.bottomLeft
                            ).map {
                                it.toLatLng()
                            }
                        }
                        .let {
                            googleMap.addPolygon(PolygonOptions().addAll(it))
                        }
                }
            }

            geofences.forEach {
                googleMap.addMarker(
                    MarkerOptions().anchor(0.5f, 0.5f).icon(icon).position(it.latLng).snippet(it.id)
                )
                googleMap.addCircle(
                    CircleOptions().strokeColor(
                        osUtilsProvider.colorFromResource(R.color.colorHyperTrackGreenSemitransparent)
                    ).radius(it.radius?.toDouble() ?: 0.0).center(it.latLng)
                )
            }
        }
    }

    fun Location.toLatLng(): LatLng {
        return LatLng(latitude, longitude)
    }

    fun createPureTextIcon(text: String?): BitmapDescriptor? {
        val textPaint = Paint().apply {
            textSize = 50f
        } // Adapt to your needs
        val textWidth: Float = textPaint.measureText(text)
        val textHeight: Float = textPaint.getTextSize()
        val width = textWidth.toInt()
        val height = textHeight.toInt()
        val image: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        canvas.translate(0f, height.toFloat())

        // For development only:
        // Set a background in order to see the
        // full size and positioning of the bitmap.
        // Remove that for a fully transparent icon.
        canvas.drawColor(Color.LTGRAY)
        canvas.drawText(text ?: "null", 0f, 0f, textPaint)
        return BitmapDescriptorFactory.fromBitmap(image)
    }

}