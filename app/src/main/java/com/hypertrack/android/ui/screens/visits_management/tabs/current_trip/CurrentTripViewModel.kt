package com.hypertrack.android.ui.screens.visits_management.tabs.current_trip

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.util.TypedValue
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDirections
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.interactors.TripsInteractor
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.models.local.LocalTrip
import com.hypertrack.android.repository.TripCreationError
import com.hypertrack.android.repository.TripCreationSuccess
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.common.mixins.ClusterManagerMixin
import com.hypertrack.android.ui.common.mixins.GeofenceClusterItem
import com.hypertrack.android.ui.common.nullIfEmpty
import com.hypertrack.android.ui.screens.add_place.AddPlaceFragmentDirections
import com.hypertrack.android.ui.screens.add_place.AddPlaceViewModel
import com.hypertrack.android.ui.screens.select_destination.DestinationData
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragmentDirections
import com.hypertrack.android.ui.screens.visits_management.tabs.history.DeviceLocationProvider
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.BuildConfig
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import net.sharewire.googlemapsclustering.ClusterManager

class CurrentTripViewModel(
    private val tripsInteractor: TripsInteractor,
    private val placesInteractor: PlacesInteractor,
    private val osUtilsProvider: OsUtilsProvider,
    private val locationProvider: DeviceLocationProvider
) : BaseViewModel(osUtilsProvider), ClusterManagerMixin<GeofenceClusterItem> {

    private val map = MutableLiveData<GoogleMap>()

    override val exception = MediatorLiveData<Consumable<Exception>>().apply {
        addSource(tripsInteractor.errorFlow.asLiveData()) {
            postValue(it)
        }
        addSource(placesInteractor.errorFlow.asLiveData()) {
            postValue(it)
        }
    }
    private lateinit var clusterManager: ClusterManager<GeofenceClusterItem>
    private val icon = GeofenceClusterItem.createIcon(osUtilsProvider)
    private val clusterIcon = GeofenceClusterItem.createClusterIcon(osUtilsProvider)

    val trip = MediatorLiveData<LocalTrip?>().apply {
        addSource(tripsInteractor.currentTrip) {
            postValue(it)
            loadingStateBase.postValue(false)
        }
    }

    init {
        loadingStateBase.postValue(true)
        viewModelScope.launch {
            tripsInteractor.refreshTrips()
        }

        placesInteractor.geofences.observeManaged {
            map.value?.let { map ->
                updateGeofencesOnMap(map, it.values.toList())
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun onMapReady(context: Context, googleMap: GoogleMap) {
        googleMap.uiSettings.apply {
            isZoomControlsEnabled = false
            isMyLocationButtonEnabled = false
        }

        try {
            googleMap.isMyLocationEnabled = true
        } catch (e: Exception) {
            throw e
        }

        googleMap.setOnCameraIdleListener {
            placesInteractor.loadGeofencesForMap(googleMap.cameraPosition.target)
            clusterManager.onCameraIdle()
        }

        clusterManager = createClusterManager(
            context,
            googleMap,
            icon = icon,
            clusterIcon = clusterIcon
        ) {
            it.snippet.nullIfEmpty()?.let { snippet ->
                destination.postValue(
                    VisitsManagementFragmentDirections.actionVisitManagementFragmentToPlaceDetailsFragment(
                        snippet
                    )
                )
            }
        }
        placesInteractor.geofences.value?.let {
            updateGeofencesOnMap(googleMap, it.values.toList())
        }

        locationProvider.getCurrentLocation {
            it?.let {
                map.value?.moveCamera(CameraUpdateFactory.newLatLngZoom(it.toLatLng(), 15.0f))
            }
        }

        map.postValue(googleMap)
    }

    fun onWhereAreYouGoingClick() {
        destination.postValue(
            VisitsManagementFragmentDirections
                .actionVisitManagementFragmentToSelectDestinationFragment()
        )
    }

    fun onDestinationResult(destinationData: DestinationData) {
        loadingStateBase.postValue(true)
        GlobalScope.launch {
            when (val res =
                tripsInteractor.createTrip(destinationData.latLng, destinationData.address)) {
                is TripCreationSuccess -> {
                }
                is TripCreationError -> {
                    errorBase.postValue(Consumable(osUtilsProvider.getErrorMessage(res.exception)))
                }
            }
            loadingStateBase.postValue(false)
        }
    }

    fun onShareTripClick() {
        trip.value!!.views?.shareUrl?.let {
            osUtilsProvider.shareText(
                text = it,
                title = osUtilsProvider.getString(R.string.share_trip_via)
            )
        }
    }

    fun onOrderClick(id: String) {
        destination.postValue(
            VisitsManagementFragmentDirections.actionVisitManagementFragmentToOrderDetailsFragment(
                id
            )
        )
    }

    fun onCompleteClick() {
        loadingStateBase.postValue(true)
        viewModelScope.launch {
            tripsInteractor.completeTrip(trip.value!!.id)
            loadingStateBase.postValue(false)
        }
    }

    fun onAddOrderClick() {
        destination.postValue(
            VisitsManagementFragmentDirections.actionVisitManagementFragmentToAddOrderFragment(
                trip.value!!.id
            )
        )
    }

    private fun updateGeofencesOnMap(googleMap: GoogleMap, geofences: List<LocalGeofence>) {
        clusterManager.setItems(geofences.map {
            GeofenceClusterItem(it)
        })
    }

    //todo task
//    private fun onMapActive() {
//        Log.d(LiveMapFragment.TAG, "onMapActive")
//        gMap?.let {
//            if (currentMapStyle != mapStyleOptions) {
//                Log.d(LiveMapFragment.TAG, "applying active style")
//                it.setMapStyle(mapStyleOptions)
//                currentMapStyle = mapStyleOptions
//            }
//        }
//    }
//
//    private fun onMapDisabled() {
//        Log.d(LiveMapFragment.TAG, "onMapDisabled")
//        gMap?.let {
//            if (currentMapStyle != mapStyleOptionsSilver) {
//                Log.d(LiveMapFragment.TAG, "applying active style")
//                it.setMapStyle(mapStyleOptionsSilver)
//                currentMapStyle = mapStyleOptionsSilver
//            }
//        }
//    }


}