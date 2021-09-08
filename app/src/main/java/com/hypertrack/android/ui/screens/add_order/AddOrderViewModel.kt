package com.hypertrack.android.ui.screens.add_order

import com.google.android.libraries.places.api.net.PlacesClient
import com.hypertrack.android.interactors.GooglePlacesInteractor
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.ui.common.select_destination.SelectDestinationViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.history.DeviceLocationProvider
import com.hypertrack.android.utils.OsUtilsProvider


class AddOrderViewModel(
    private val tripId: String,
    private val placesInteractor: PlacesInteractor,
    private val googlePlacesInteractor: GooglePlacesInteractor,
    private val deviceLocationProvider: DeviceLocationProvider,
    private val osUtilsProvider: OsUtilsProvider,
) : SelectDestinationViewModel(
    placesInteractor,
    googlePlacesInteractor,
    osUtilsProvider,
    deviceLocationProvider
) {
    override fun proceed(destinationData: DestinationData) {
        destination.postValue(
            AddOrderFragmentDirections.actionAddOrderFragmentToAddOrderInfoFragment(
                destinationData, tripId
            )
        )
    }
}