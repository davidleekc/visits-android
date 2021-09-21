package com.hypertrack.android.ui.screens.add_place

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.hypertrack.android.interactors.GooglePlacesInteractor
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.base.ErrorHandler
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.ui.common.select_destination.SelectDestinationViewModel
import com.hypertrack.android.ui.common.select_destination.reducer.Proceed
import com.hypertrack.android.ui.common.select_destination.reducer.Reset
import com.hypertrack.android.ui.common.select_destination.toDestinationData
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

    override fun handleEffect(proceed: Proceed) {
        val destinationData = proceed.placeData.toDestinationData()
        viewModelScope.launch {
            loadingStateBase.postValue(true)
            val has = placesInteractor.hasAdjacentGeofence(destinationData.latLng)
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
        sendAction(Reset)
    }

    override fun proceed(destinationData: DestinationData) {
        destination.postValue(
            AddPlaceFragmentDirections.actionAddPlaceFragmentToAddPlaceInfoFragment(
                destinationData
            )
        )
    }

}