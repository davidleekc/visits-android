package com.hypertrack.android.ui.screens.add_place

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Circle
import com.hypertrack.android.interactors.GooglePlacesInteractor
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.base.ErrorHandler
import com.hypertrack.android.ui.common.HypertrackMapWrapper
import com.hypertrack.android.ui.common.delegates.GeofenceClusterItem
import com.hypertrack.android.ui.common.delegates.GeofencesMapDelegate
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.ui.common.select_destination.SelectDestinationViewModel
import com.hypertrack.android.ui.common.select_destination.reducer.MapReady
import com.hypertrack.android.ui.common.select_destination.reducer.Proceed
import com.hypertrack.android.ui.common.select_destination.toDestinationData
import com.hypertrack.android.ui.screens.add_integration.AddIntegrationViewModel
import com.hypertrack.android.ui.screens.add_place_info.AddPlaceInfoViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.history.DeviceLocationProvider
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.OsUtilsProvider
import kotlinx.coroutines.launch


class AddPlaceViewModel(
    private val placesInteractor: PlacesInteractor,
    private val googlePlacesInteractor: GooglePlacesInteractor,
    private val osUtilsProvider: OsUtilsProvider,
    private val deviceLocationProvider: DeviceLocationProvider,
    private val crashReportsProvider: CrashReportsProvider
) : SelectDestinationViewModel(
    placesInteractor,
    googlePlacesInteractor,
    osUtilsProvider,
    deviceLocationProvider,
    crashReportsProvider
) {
    val adjacentGeofenceDialog = MutableLiveData<Consumable<DestinationData>>()

    override val loadingStateBase = placesInteractor.isLoadingForLocation

    override val errorHandler =
        ErrorHandler(osUtilsProvider, placesInteractor.errorFlow.asLiveData())

    private var radiusCircle: Circle? = null

    override val defaultZoom: Float = 16f

    override fun handleEffect(proceed: Proceed) {
        val destinationData = proceed.placeData.toDestinationData()
        viewModelScope.launch {
            loadingStateBase.postValue(true)
            val has = placesInteractor.hasAdjacentGeofence(
                destinationData.latLng,
                PlacesInteractor.DEFAULT_RADIUS
            )
            loadingStateBase.postValue(false)
            if (has) {
                adjacentGeofenceDialog.postValue(Consumable(destinationData))
                return@launch
            } else {
                proceed(destinationData)
            }
        }
    }

    fun onGeofenceDialogYes(destinationData: DestinationData) {
        proceed(destinationData)
    }

    fun onGeofenceDialogNo() {
    }

    override fun createGeofencesMapDelegate(
        context: Context,
        wrapper: HypertrackMapWrapper,
        markerClickListener: (GeofenceClusterItem) -> Unit
    ): GeofencesMapDelegate {
        return object : GeofencesMapDelegate(
            context,
            wrapper,
            placesInteractor,
            osUtilsProvider,
            markerClickListener
        ) {
            override fun updateGeofencesOnMap(
                mapWrapper: HypertrackMapWrapper,
                geofences: List<LocalGeofence>
            ) {
                super.updateGeofencesOnMap(mapWrapper, geofences)
                displayRadius()
            }
        }
    }

    override fun onCameraMoved(map: GoogleMap) {
        super.onCameraMoved(map)
        displayRadius()
    }

    override fun proceed(destinationData: DestinationData) {
        destination.postValue(
            AddPlaceFragmentDirections.actionAddPlaceFragmentToAddPlaceInfoFragment(
                destinationData
            )
        )
    }

    private fun displayRadius() {
        val state = state
        if (state is MapReady) {
            radiusCircle?.remove()
            radiusCircle = state.map.addNewGeofenceRadius(
                state.map.cameraPosition,
                PlacesInteractor.DEFAULT_RADIUS
            )
        }
    }

}