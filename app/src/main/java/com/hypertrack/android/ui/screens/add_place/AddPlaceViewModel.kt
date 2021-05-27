package com.hypertrack.android.ui.screens.add_place

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.hypertrack.android.models.Location
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.SingleLiveEvent
import com.hypertrack.android.ui.base.ZipLiveData
import com.hypertrack.android.ui.common.toAddressString
import com.hypertrack.android.ui.screens.select_destination.SelectDestinationViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.history.DeviceLocationProvider
import com.hypertrack.android.utils.OsUtilsProvider


class AddPlaceViewModel(
    private val osUtilsProvider: OsUtilsProvider,
    private val placesClient: PlacesClient,
    private val deviceLocationProvider: DeviceLocationProvider,
) : SelectDestinationViewModel(
    osUtilsProvider,
    placesClient,
    deviceLocationProvider
) {

    protected override fun proceed(latLng: LatLng, address: String?) {
        destination.postValue(
            AddPlaceFragmentDirections.actionAddPlaceFragmentToAddPlaceInfoFragment(
                latLng,
                address = address,
                name = currentPlace?.name
            )
        )
    }

}