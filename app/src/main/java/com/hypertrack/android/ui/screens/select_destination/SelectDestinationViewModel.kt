package com.hypertrack.android.ui.screens.select_destination

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.interactors.PlacesInteractorImpl
import com.hypertrack.android.models.Location
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.SingleLiveEvent
import com.hypertrack.android.ui.base.ZipLiveData
import com.hypertrack.android.ui.common.delegates.GeofenceClusterItem
import com.hypertrack.android.ui.common.delegates.GeofencesMapDelegate
import com.hypertrack.android.ui.common.nullIfEmpty
import com.hypertrack.android.ui.common.toAddressString
import com.hypertrack.android.ui.common.toLatLng
import com.hypertrack.android.ui.screens.add_place.AddPlaceFragmentDirections
import com.hypertrack.android.ui.screens.visits_management.tabs.history.DeviceLocationProvider
import com.hypertrack.android.utils.OsUtilsProvider
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import net.sharewire.googlemapsclustering.ClusterManager


open class SelectDestinationViewModel(
    private val placesInteractor: PlacesInteractor,
    private val osUtilsProvider: OsUtilsProvider,
    private val placesClient: PlacesClient,
    private val deviceLocationProvider: DeviceLocationProvider,
) : BaseViewModel() {

    private var firstLaunch: Boolean = true
    private var programmaticCameraMove: Boolean = false
    private val currentLocation = MutableLiveData<Location>()
    val places = MutableLiveData<List<GooglePlaceModel>>()
    val map = MutableLiveData<GoogleMap>()
    val searchText = MutableLiveData<String>()
    val error = SingleLiveEvent<String>()
    val closeKeyboard = SingleLiveEvent<Boolean>()

    val goBackEvent = SingleLiveEvent<DestinationData>()

    protected var currentPlace: Place? = null

    //todo persist token?
    private var token: AutocompleteSessionToken? = null
    private var bias: RectangularBounds? = null

    private lateinit var geofencesMapDelegate: GeofencesMapDelegate

    init {
        deviceLocationProvider.getCurrentLocation {
            currentLocation.postValue(it)
        }
    }

    init {
        ZipLiveData(currentLocation, map).apply {
            //todo check leak
            observeManaged { pair ->
                if (map.value!!.cameraPosition.target.latitude
                    < 0.1 && map.value!!.cameraPosition.target.longitude < 0.1
                ) {
                    pair.first.let { location ->
                        map.value!!.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(
                                    location.latitude,
                                    location.longitude
                                ), 13f
                            )
                        )

                        bias = RectangularBounds.newInstance(
                            LatLng(location.latitude - 0.1, location.longitude + 0.1),  // SW
                            LatLng(location.latitude + 0.1, location.longitude - 0.1) // NE
                        )
                    }

                }
            }
        }

    }


    @SuppressLint("MissingPermission")
    open fun onMapReady(context: Context, googleMap: GoogleMap) {
        map.postValue(googleMap)

        try {
            googleMap.isMyLocationEnabled = true
        } catch (_: Exception) {
        }

        googleMap.setOnCameraIdleListener {
            map.value?.let {
                onCameraMoved(it)
            }
            if (/*!firstLaunch &&*/ !programmaticCameraMove) {
                map.value?.cameraPosition?.target?.let {
                    currentPlace = null
                    searchText.postValue(
                        osUtilsProvider.getPlaceFromCoordinates(
                            it.latitude,
                            it.longitude,
                        )?.toAddressString()
                    )
                }
            }
            firstLaunch = false
            programmaticCameraMove = false
        }

        googleMap.uiSettings.apply {
            isMyLocationButtonEnabled = true
            isZoomControlsEnabled = true
        }

        googleMap.setOnMapClickListener {
            places.postValue(listOf())
            closeKeyboard.postValue(true)
        }

        geofencesMapDelegate = GeofencesMapDelegate(
            context,
            googleMap,
            placesInteractor,
            osUtilsProvider
        ) {
            it.snippet.nullIfEmpty()?.let { snippet ->
                destination.postValue(
                    AddPlaceFragmentDirections.actionAddPlaceFragmentToPlaceDetailsFragment(
                        snippet
                    )
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        currentPlace = null
        // Create a new token for the autocomplete session. Pass this to FindAutocompletePredictionsRequest,
        // and once again when the user makes a selection (for example when calling selectPlace()).
        if (token == null) {
            token = AutocompleteSessionToken.newInstance()
        }

        val requestBuilder = FindAutocompletePredictionsRequest.builder()
//                .setTypeFilter(TypeFilter.ADDRESS)
            .setSessionToken(token)
            .setQuery(query)
            .setLocationBias(bias)
        currentLocation.value?.let {
            requestBuilder.setOrigin(LatLng(it.latitude, it.longitude))
        }

        placesClient.findAutocompletePredictions(requestBuilder.build())
            .addOnSuccessListener { response ->
                places.postValue(GooglePlaceModel.from(response.autocompletePredictions))
            }
            .addOnFailureListener { e ->
                places.postValue(listOf())
                error.postValue(e.message)
            }
    }

    open fun onConfirmClicked(address: String) {
        proceed(
            DestinationData(
                map.value!!.cameraPosition.target,
                address = address,
                name = currentPlace?.name
            )
        )
    }

    fun onPlaceItemClick(item: GooglePlaceModel) {
        val placeFields: List<Place.Field> =
            listOf(
                Place.Field.ID,
                Place.Field.ADDRESS,
                Place.Field.NAME,
                Place.Field.ADDRESS_COMPONENTS,
                Place.Field.LAT_LNG
            )
        val request = FetchPlaceRequest.newInstance(item.placeId, placeFields)

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response: FetchPlaceResponse ->
                val place = response.place
                place.latLng?.let { ll ->
                    map.value!!.let {
                        currentPlace = place
                        places.postValue(listOf())
                        searchText.postValue(place.toAddressString())
                        moveMapCamera(ll.latitude, ll.longitude)
                    }
                }
            }
            .addOnFailureListener { exception: java.lang.Exception ->
                //todo handle error
            }
    }

    protected open fun proceed(destinationData: DestinationData) {
        goBackEvent.postValue(destinationData)
    }

    protected open fun onCameraMoved(map: GoogleMap) {
        val region = map.projection.visibleRegion
        placesInteractor.loadGeofencesForMap(map.cameraPosition.target)
        geofencesMapDelegate.onCameraIdle()
    }

    private fun moveMapCamera(latitude: Double, longitude: Double) {
        programmaticCameraMove = true
        map.value!!.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(
                    latitude,
                    longitude
                ), 13f
            )
        )
    }

    companion object {
        const val SHOW_DEBUG_DATA = false
    }

}