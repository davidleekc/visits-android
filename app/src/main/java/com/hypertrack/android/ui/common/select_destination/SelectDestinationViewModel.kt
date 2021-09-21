package com.hypertrack.android.ui.common.select_destination

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.hypertrack.android.interactors.GooglePlacesInteractor
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.SingleLiveEvent
import com.hypertrack.android.ui.common.*
import com.hypertrack.android.ui.common.delegates.GeofencesMapDelegate
import com.hypertrack.android.ui.common.delegates.GooglePlaceAddressDelegate
import com.hypertrack.android.ui.common.select_destination.reducer.*
import com.hypertrack.android.ui.common.util.nullIfEmpty
import com.hypertrack.android.ui.screens.add_place.AddPlaceFragmentDirections
import com.hypertrack.android.ui.screens.visits_management.tabs.history.DeviceLocationProvider
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.asNonEmpty
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

open class SelectDestinationViewModel(
    private val placesInteractor: PlacesInteractor,
    private val googlePlacesInteractor: GooglePlacesInteractor,
    private val osUtilsProvider: OsUtilsProvider,
    private val deviceLocationProvider: DeviceLocationProvider,
    private val crashReportsProvider: CrashReportsProvider
) : BaseViewModel(osUtilsProvider) {

    private val reducer = SelectDestinationViewModelReducer()
    private lateinit var state: State

    private val addressDelegate = GooglePlaceAddressDelegate(osUtilsProvider)
    private val placesDelegate = GooglePlacesSearchDelegate(googlePlacesInteractor)
    private lateinit var geofencesMapDelegate: GeofencesMapDelegate

    private var programmaticCameraMove: Boolean = false

    val address = MutableLiveData<String>()
    val showConfirmButton = MutableLiveData<Boolean>()
    val placesResults = MutableLiveData<List<GooglePlaceModel>>()
    val closeKeyboard = SingleLiveEvent<Boolean>()
    val goBackEvent = SingleLiveEvent<DestinationData>()
    val removeSearchFocusEvent = SingleLiveEvent<Boolean>()

    init {
        deviceLocationProvider.getCurrentLocation {
            it?.let {
                sendAction(UserLocation(it.toLatLng()))
            }
        }
    }

    protected fun sendAction(action: Action) {
        viewModelScope.launch {
            val actionLog = "action = $action"
//            Log.v("hypertrack-verbose", actionLog)
            crashReportsProvider.log(actionLog)
            try {
                val res = reducer.reduceAction(state, action)
                applyState(res.newState)
                applyEffects(res.effects)
            } catch (e: Exception) {
                if (MyApplication.DEBUG_MODE) {
                    throw e
                } else {
                    errorHandler.postException(e)
                    crashReportsProvider.logException(e)
                }
            }
        }
    }

    private fun applyState(state: State) {
        when (state) {
            is Initial -> {
                showConfirmButton.postValue(true)
            }
            is AutocompleteIsActive -> {
                placesResults.postValue(state.places.elements)
                showConfirmButton.postValue(false)
            }
            is MapIsActive -> {
                placesResults.postValue(listOf())
                showConfirmButton.postValue(true)
            }
            is Confirmed -> {
                showConfirmButton.postValue(false)
            }
        }.let { }
        this.state = state
        val stateLog = "new state = $state"
//        Log.v("hypertrack-verbose", stateLog)
        crashReportsProvider.log(stateLog)
    }

    private fun applyEffects(effects: Set<Effect>) {
        for (effect in effects) {
            val effectLog = "effect = $effect"
//            Log.v("hypertrack-verbose", effectLog)
            crashReportsProvider.log(effectLog)
            when (effect) {
                is DisplayAddress -> {
                    address.postValue(effect.address)
                }
                CloseKeyboard -> {
                    closeKeyboard.postValue(true)
                }
                is MoveMap -> {
                    moveMapCamera(effect.map, effect.latLng)
                }
                is Proceed -> {
                    handleEffect(effect)
                }
                RemoveSearchFocus -> {
                    removeSearchFocusEvent.postValue(true)
                }
            }.let {}
        }
    }

    fun onViewCreated() {
        state = SelectDestinationViewModelReducer.INITIAL_STATE
    }

    @SuppressLint("MissingPermission")
    open fun onMapReady(context: Context, googleMap: GoogleMap) {
        val wrapper = HypertrackMapWrapper(googleMap, osUtilsProvider)

        //todo settings to wrapper
        try {
            googleMap.isMyLocationEnabled = true
        } catch (_: Exception) {
        }

        googleMap.setOnCameraIdleListener {
            onCameraMoved(googleMap)
            sendAction(
                MapCameraMoved(
                    googleMap.viewportPosition,
                    googleMap.viewportPosition.let {
                        addressDelegate.displayAddress(
                            osUtilsProvider.getPlaceFromCoordinates(
                                it
                            )
                        )
                    },
                    programmaticCameraMove
                )
            )
            programmaticCameraMove = false
        }

        googleMap.uiSettings.apply {
            isMyLocationButtonEnabled = true
            isZoomControlsEnabled = true
        }

        googleMap.setOnMapClickListener {
            sendAction(
                MapClicked(
                    googleMap.viewportPosition,
                    osUtilsProvider.getPlaceFromCoordinates(googleMap.viewportPosition).let {
                        addressDelegate.displayAddress(it)
                    })
            )
        }

        geofencesMapDelegate = GeofencesMapDelegate(
            context,
            wrapper,
            placesInteractor,
            osUtilsProvider
        ) {
            it.snippet.nullIfEmpty()?.let { snippet ->
                destination.postValue(
                    AddPlaceFragmentDirections.actionGlobalPlaceDetailsFragment(
                        snippet
                    )
                )
            }
        }
        sendAction(MapReadyAction(
            wrapper,
            googleMap.viewportPosition,
            googleMap.viewportPosition.let {
                addressDelegate.displayAddress(osUtilsProvider.getPlaceFromCoordinates(it))
            }
        ))
    }

    fun onSearchQueryChanged(query: String) {
        viewModelScope.launch {
            try {
                val res = viewModelScope.async {
                    placesDelegate.search(query, state.userLocation)
                }.await()
                if (res.isNotEmpty()) {
                    sendAction(SearchQueryChanged(query, res.asNonEmpty()))
                }
            } catch (e: Exception) {
                errorHandler.postException(e)
                sendAction(AutocompleteError(query))
            }
        }
    }

    fun onConfirmClicked() {
        sendAction(ConfirmClicked)
    }

    fun onPlaceItemClick(item: GooglePlaceModel) {
        viewModelScope.launch {
            placesDelegate.fetchPlace(item).let { place ->
                place.latLng?.let { ll ->
                    sendAction(
                        PlaceSelectedAction(
                            displayAddress = addressDelegate.displayAddress(place),
                            strictAddress = addressDelegate.strictAddress(place),
                            name = place.name,
                            ll
                        )
                    )
                }
            }
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

    protected open fun handleEffect(proceed: Proceed) {
        proceed(proceed.placeData.toDestinationData())
    }

    private fun moveMapCamera(map: HypertrackMapWrapper, latLng: LatLng) {
        programmaticCameraMove = true
        map.moveCamera(latLng)
    }

}

fun PlaceData.toDestinationData(): DestinationData {
    val placeData = this
    return when (placeData) {
        is PlaceSelected -> {
            DestinationData(
                placeData.latLng,
                address = placeData.strictAddress,
                name = placeData.name
            )
        }
        is LocationSelected -> {
            DestinationData(
                placeData.latLng,
                address = placeData.address,
                name = null
            )
        }
        is LocationSelectedWithUnsuccesfulQuery -> {
            DestinationData(
                placeData.mapState.cameraPosition,
                address = placeData.mapState.cameraPositionAddress,
                name = null
            )
        }
    }
}


