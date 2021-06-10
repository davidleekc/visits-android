package com.hypertrack.android.ui.screens.add_place

import android.annotation.SuppressLint
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.models.Location
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.SingleLiveEvent
import com.hypertrack.android.ui.base.ZipLiveData
import com.hypertrack.android.ui.common.toAddressString
import com.hypertrack.android.ui.screens.select_destination.SelectDestinationViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.history.DeviceLocationProvider
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.R


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
            destination.postValue(
                AddPlaceFragmentDirections.actionAddPlaceFragmentToPlaceDetailsFragment(
                    it.snippet
                )
            )
            true
        }
    }

    private fun onDisplayGeofencesOnMap(googleMap: GoogleMap, geofences: List<LocalGeofence>) {
        googleMap.clear()
        geofences.forEach {
            googleMap.addMarker(
                MarkerOptions().anchor(0.5f, 0.5f).position(it.latLng).icon(
                    BitmapDescriptorFactory.fromBitmap(
                        osUtilsProvider.bitmapFormResource(
                            R.drawable.ic_ht_departure_active
                        )
                    )
                ).snippet(it.id)
            )
            googleMap.addCircle(
                CircleOptions().strokeColor(
                    osUtilsProvider.colorFromResource(R.color.colorHyperTrackGreenSemitransparent)
                ).radius(it.radius?.toDouble() ?: 0.0).center(it.latLng)
            )
        }
    }

}