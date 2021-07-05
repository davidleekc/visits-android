package com.hypertrack.android.ui.screens.add_place

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.places.api.net.PlacesClient
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.ui.common.select_destination.SelectDestinationViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.history.DeviceLocationProvider
import com.hypertrack.android.utils.OsUtilsProvider
import kotlinx.coroutines.launch


class AddPlaceViewModel(
    private val osUtilsProvider: OsUtilsProvider,
    private val placesClient: PlacesClient,
    private val deviceLocationProvider: DeviceLocationProvider,
    private val placesInteractor: PlacesInteractor
) : SelectDestinationViewModel(
    placesInteractor,
    osUtilsProvider,
    placesClient,
    deviceLocationProvider
) {

    val adjacentGeofenceDialog = MutableLiveData<Consumable<DestinationData>>()

    override val loadingStateBase = placesInteractor.isLoadingForLocation

    override val errorBase = MediatorLiveData<Consumable<String>>().apply {
        addSource(placesInteractor.errorFlow.asLiveData()) { e ->
            postValue(e.map {
                osUtilsProvider.getErrorMessage(it)
            })
        }
    }

    override fun onConfirmClicked() {
        proceedCreation(
            DestinationData(
                map.value!!.cameraPosition.target,
                address = getAddress(),
                name = currentPlace?.name
            )
        )
    }

    fun proceedCreation(destinationData: DestinationData, confirmed: Boolean = false) {
        viewModelScope.launch {
            if (!confirmed) {
                loadingStateBase.postValue(true)
                val has = placesInteractor.hasAdjacentGeofence(destinationData.latLng)
                loadingStateBase.postValue(false)
                if (has) {
                    adjacentGeofenceDialog.postValue(Consumable(destinationData))
                    return@launch
                } else {
                    proceed(destinationData)
                }
            } else {
                proceed(destinationData)
            }
        }
    }

    override fun proceed(destinationData: DestinationData) {
        destination.postValue(
            AddPlaceFragmentDirections.actionAddPlaceFragmentToAddPlaceInfoFragment(
                destinationData
            )
        )
    }


}