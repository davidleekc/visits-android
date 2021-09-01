package com.hypertrack.android.ui.screens.visits_management.tabs.current_trip

import android.annotation.SuppressLint
import android.content.Context
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
import com.hypertrack.android.ui.base.ZipLiveData
import com.hypertrack.android.ui.base.ZipNotNullableLiveData
import com.hypertrack.android.ui.common.HypertrackMapWrapper
import com.hypertrack.android.ui.common.delegates.GeofencesMapDelegate
import com.hypertrack.android.ui.common.util.nullIfEmpty
import com.hypertrack.android.ui.common.util.requireValue
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.ui.common.util.updateValue
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragmentDirections
import com.hypertrack.android.ui.screens.visits_management.tabs.history.DeviceLocationProvider
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.HyperTrackService
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class CurrentTripViewModel(
    private val tripsInteractor: TripsInteractor,
    private val placesInteractor: PlacesInteractor,
    private val hyperTrackService: HyperTrackService,
    private val locationProvider: DeviceLocationProvider,
    private val osUtilsProvider: OsUtilsProvider,
    private val crashReportsProvider: CrashReportsProvider
) : BaseViewModel(osUtilsProvider) {

    private val isTracking = MediatorLiveData<Boolean>().apply {
        addSource(hyperTrackService.isTracking) {
            if (value != it) {
                updateValue(it)
            }
        }
    }

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
    val showWhereAreYouGoing: LiveData<Boolean> =
        ZipLiveData(hyperTrackService.isTracking, trip).let {
            Transformations.map(it) { (isTracking, trip) ->
                return@map if (isTracking != null) {
                    trip == null && isTracking
                } else {
                    false
                }
            }
        }
    val mapActiveState: LiveData<Boolean?> = ZipNotNullableLiveData(isTracking, map).let {
        Transformations.map(it) { (isTracking, map) ->
            isTracking
        }
    }

    init {
        trip.addSource(tripsInteractor.currentTrip) {
            if (isTracking.requireValue()) {
                trip.postValue(it)
                map.value?.let { map -> displayTripOnMap(map, it) }
            }
        }
        trip.addSource(map) {
            if (isTracking.requireValue()) {
                displayTripOnMap(it, trip.value)
            }
        }
        trip.addSource(isTracking) {
            if (it) {
                map.value?.let {
                    tripsInteractor.currentTrip.value?.let { trip ->
                        this.trip.postValue(trip)
                        displayTripOnMap(map.requireValue(), trip)
                    }
                }
            } else {
                trip.postValue(null)
            }
        }

        mapActiveState.observeManaged {
            if (it != null) {
                //todo check geofences delegate
                if (it) {
                    val trip = tripsInteractor.currentTrip
                    if (trip.value != null) {
                        animateMapCameraToTrip(trip.value!!, map.requireValue())
                    } else {
                        if (userLocation.value != null) {
                            animateMapCameraToUser(map.requireValue(), userLocation.value!!)
                        }
                    }
                } else {
                    map.requireValue().clear()
                    if (userLocation.value != null) {
                        animateMapCameraToUser(map.requireValue(), userLocation.value!!)
                    }
                }
            }
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

    fun onViewCreated() {
        if (loadingStateBase.value != true) {
            viewModelScope.launch {
                tripsInteractor.refreshTrips()
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
            geofencesMapDelegate.onCameraIdle()
        }

        geofencesMapDelegate = object : GeofencesMapDelegate(
            context,
            HypertrackMapWrapper(googleMap, osUtilsProvider),
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
                mapWrapper: HypertrackMapWrapper,
                geofences: List<LocalGeofence>
            ) {
                if (trip.value == null) {
                    super.updateGeofencesOnMap(mapWrapper, geofences)
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
                .actionVisitManagementFragmentToSelectTripDestinationFragment()
        )
    }

    fun onDestinationResult(destinationData: DestinationData) {
        loadingStateBase.updateValue(true)
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

    fun onMyLocationClick() {
        if (map.value != null && userLocation.value != null) {
            animateMapCameraToUser(map.requireValue(), userLocation.value!!)
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (this::geofencesMapDelegate.isInitialized) {
            geofencesMapDelegate.onCleared()
        }
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

            animateMapCameraToTrip(trip, map)
        }
    }

    private fun animateMapCameraToTrip(trip: LocalTrip, map: GoogleMap) {
        try {
            val tripStart =
                trip.orders.firstOrNull()?.estimate?.route?.polyline?.getPolylinePoints()
                    ?.firstOrNull()

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
                //newLatLngBounds can cause crash if called before layout without map size
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            }
        } catch (e: Exception) {
            crashReportsProvider.logException(e)
        }
    }

    private fun animateMapCameraToUser(map: GoogleMap, userLocation: LatLng) {
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                userLocation,
                DEFAULT_ZOOM
            )
        )
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

    companion object {
        const val DEFAULT_ZOOM = 15f
    }

}