package com.hypertrack.android.ui.screens.visits_management.tabs.current_trip

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.hypertrack.android.interactors.TripsInteractor
import com.hypertrack.android.repository.TripCreationError
import com.hypertrack.android.repository.TripCreationSuccess
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.base.toConsumable
import com.hypertrack.android.ui.screens.select_destination.DestinationData
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragmentDirections
import com.hypertrack.android.utils.OsUtilsProvider
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class CurrentTripViewModel(
    private val tripsInteractor: TripsInteractor,
    private val osUtilsProvider: OsUtilsProvider
) : BaseViewModel() {

    val trip = tripsInteractor.currentTrip

    val showWhereAreYouGoing = MediatorLiveData<Boolean>().apply {
        addSource(tripsInteractor.currentTrip) {
            loadingStateBase.postValue(false)
            postValue(it == null)
        }
    }

    init {
        showWhereAreYouGoing.postValue(false)
        loadingStateBase.postValue(true)
        viewModelScope.launch {
            tripsInteractor.refreshTrips()
        }
    }

    fun onWhereAreYouGoingClick() {
        destination.postValue(
            VisitsManagementFragmentDirections
                .actionVisitManagementFragmentToSelectDestinationFragment()
        )
    }

    fun onDestinationResult(destinationData: DestinationData) {
        GlobalScope.launch {
            loadingStateBase.postValue(true)
            when (val res = tripsInteractor.createTrip(destinationData.latLng)) {
                is TripCreationSuccess -> {
                    errorBase.postValue(Consumable("Trip created"))
                }
                is TripCreationError -> {
                    errorBase.postValue(Consumable(osUtilsProvider.getErrorMessage(res.exception)))
                }
            }
            loadingStateBase.postValue(false)
        }
    }

}