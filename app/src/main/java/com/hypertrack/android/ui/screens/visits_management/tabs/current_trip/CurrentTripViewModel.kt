package com.hypertrack.android.ui.screens.visits_management.tabs.current_trip

import androidx.lifecycle.Transformations
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

    val showWhereAreYouGoing = Transformations.map(tripsInteractor.currentTrip) {
        it == null
    }

    fun onWhereAreYouGoingClick() {
        destination.postValue(
            VisitsManagementFragmentDirections
                .actionVisitManagementFragmentToSelectDestinationFragment()
        )
    }

    fun onDestinationResult(destinationData: DestinationData) {
        GlobalScope.launch {
            when (val res = tripsInteractor.createTrip(destinationData.latLng)) {
                is TripCreationSuccess -> {
                    errorBase.postValue(Consumable("Trip created"))
                }
                is TripCreationError -> {
                    errorBase.postValue(Consumable(osUtilsProvider.getErrorMessage(res.exception)))
                }
            }
        }
    }

}