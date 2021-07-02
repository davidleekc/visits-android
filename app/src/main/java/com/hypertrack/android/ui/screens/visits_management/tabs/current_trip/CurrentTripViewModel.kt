package com.hypertrack.android.ui.screens.visits_management.tabs.current_trip

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.util.TypedValue
import androidx.lifecycle.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.interactors.TripsInteractor
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.models.local.LocalTrip
import com.hypertrack.android.models.local.OrderStatus
import com.hypertrack.android.repository.TripCreationError
import com.hypertrack.android.repository.TripCreationSuccess
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.common.delegates.GeofencesMapDelegate
import com.hypertrack.android.ui.common.nullIfEmpty
import com.hypertrack.android.ui.screens.select_destination.DestinationData
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragmentDirections
import com.hypertrack.android.ui.screens.visits_management.tabs.history.DeviceLocationProvider
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class CurrentTripViewModel(
    private val tripsInteractor: TripsInteractor,
    private val placesInteractor: PlacesInteractor,
    private val osUtilsProvider: OsUtilsProvider,
    private val locationProvider: DeviceLocationProvider
) : BaseViewModel(osUtilsProvider) {

    private val map = MutableLiveData<GoogleMap>()
    private lateinit var geofencesMapDelegate: GeofencesMapDelegate

    override val exception = MediatorLiveData<Consumable<Exception>>().apply {
        addSource(tripsInteractor.errorFlow.asLiveData()) {
            postValue(it)
        }
        addSource(placesInteractor.errorFlow.asLiveData()) {
            postValue(it)
        }
    }
    val trip = MediatorLiveData<LocalTrip?>()
    val userLocation = MutableLiveData<LatLng?>()

    init {
        trip.addSource(tripsInteractor.currentTrip) {
            trip.postValue(it)
            map.value?.let { map -> displayTripOnMap(map, it) }
        }
        trip.addSource(map) {
            displayTripOnMap(it, trip.value)
        }
    }

    init {
        viewModelScope.launch {
            tripsInteractor.refreshTrips()
        }
        locationProvider.getCurrentLocation {
            userLocation.postValue(it?.toLatLng())
        }
    }

    val tripStartIcon = osUtilsProvider.bitmapDescriptorFromResource(
        com.hypertrack.maps.google.R.drawable.starting_position
    )
    val activeOrderIcon = osUtilsProvider.bitmapDescriptorFromResource(
        com.hypertrack.maps.google.R.drawable.destination
    )
    val completedOrderIcon = osUtilsProvider.bitmapDescriptorFromVectorResource(
        R.drawable.ic_order_completed, R.color.colorHyperTrackGreen
    )
    val canceledOrderIcon = osUtilsProvider.bitmapDescriptorFromVectorResource(
        R.drawable.ic_order_canceled, R.color.colorHyperTrackGreen
    )

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
            geofencesMapDelegate.onCameraIdle()
        }

        geofencesMapDelegate = object : GeofencesMapDelegate(
            context,
            googleMap,
            placesInteractor,
            osUtilsProvider,
            {
                it.snippet.nullIfEmpty()?.let { snippet ->
                    destination.postValue(
                        VisitsManagementFragmentDirections.actionVisitManagementFragmentToPlaceDetailsFragment(
                            snippet
                        )
                    )
                }
            }
        ) {
            override fun updateGeofencesOnMap(
                googleMap: GoogleMap,
                geofences: List<LocalGeofence>
            ) {
                if (trip.value == null) {
                    super.updateGeofencesOnMap(googleMap, geofences)
                }
            }

            override fun onCameraIdle() {
                if (trip.value == null) {
                    super.onCameraIdle()
                }
            }
        }

        this.userLocation.observeManaged {
            if (trip.value == null && it != null) {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it, DEFAULT_ZOOM))
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
        Log.v("hypertrack-verbose", "true")
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
            Log.v("hypertrack-verbose", "false")
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

    fun onMyLocationClick() {
        if (map.value != null && userLocation.value != null) {
            map.value!!.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    userLocation.value!!,
                    DEFAULT_ZOOM
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        geofencesMapDelegate.onCleared()
    }

    private fun displayTripOnMap(map: GoogleMap, trip: LocalTrip?) {
        map.clear()
        trip?.let { trip ->
            val tripStart =
                trip.orders.firstOrNull()?.estimate?.route?.polyline?.getPolylinePoints()
                    ?.firstOrNull()

            tripStart?.let {
                map.addMarker(
                    MarkerOptions()
                        .position(it)
                        .anchor(0.5f, 0.5f)
                        .icon(tripStartIcon)
                        .zIndex(100f)
                )
            }

            trip.ongoingOrders.forEach { order ->
                order.estimate?.route?.polyline?.getPolylinePoints()?.let {
                    val options = if (order.status == OrderStatus.ONGOING) {
                        PolylineOptions()
                            .width(tripStyleAttrs.tripRouteWidth)
                            .color(tripStyleAttrs.tripRouteColor)
                            .pattern(
                                Arrays.asList(
                                    Dash(tripStyleAttrs.tripRouteWidth * 2),
                                    Gap(tripStyleAttrs.tripRouteWidth)
                                )
                            )
                    } else {
                        PolylineOptions()
                            .width(tripStyleAttrs.tripRouteWidth)
                            .color(tripStyleAttrs.tripRouteColor)
                            .pattern(
                                Arrays.asList(
                                    Dash(tripStyleAttrs.tripRouteWidth * 2),
                                    Gap(tripStyleAttrs.tripRouteWidth)
                                )
                            )
                    }

                    map.addPolyline(options.addAll(it))
                }

                map.addMarker(
                    MarkerOptions()
                        .anchor(0.5f, 0.5f)
                        .icon(
                            when (order.status) {
                                OrderStatus.ONGOING -> {
                                    activeOrderIcon
                                }
                                OrderStatus.COMPLETED -> {
                                    completedOrderIcon
                                }
                                OrderStatus.CANCELED, OrderStatus.UNKNOWN -> {
                                    canceledOrderIcon
                                }
                            }
                        )
                        .position(order.destinationLatLng)
                        .zIndex(100f)
                )


            }

            if (trip.ongoingOrders.isNotEmpty()) {
                val bounds = LatLngBounds.builder().apply {
                    trip.ongoingOrders.forEach { order ->
                        include(order.destinationLatLng)
                        order.estimate?.route?.polyline?.getPolylinePoints()?.forEach {
                            include(it)
                        }
                    }
                    tripStart?.let { include(it) }
                    this@CurrentTripViewModel.userLocation.value?.let { include(it) }
                }.build()
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            }
        }
    }

    private val tripStyleAttrs by lazy {
        StyleAttrs().let { tripStyleAttrs ->
            tripStyleAttrs.tripRouteWidth = tripRouteWidth
            tripStyleAttrs.tripRouteColor =
                osUtilsProvider.colorFromResource(com.hypertrack.maps.google.R.color.ht_route)
            tripStyleAttrs
        }
    }

    val tripRouteWidth by lazy {
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 3f,
            osUtilsProvider.getDisplayMetrics()
        )
    }
    val accuracyStrokeWidth by lazy {
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 1f,
            osUtilsProvider.getDisplayMetrics()
        )
    }

    private class StyleAttrs {
        var tripRouteWidth = 0f
        var tripRouteColor = 0
    }

    //todo
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

    companion object {
        const val DEFAULT_ZOOM = 15f
    }


}