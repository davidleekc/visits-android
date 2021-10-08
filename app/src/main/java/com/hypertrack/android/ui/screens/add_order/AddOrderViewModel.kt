package com.hypertrack.android.ui.screens.add_order

import com.google.android.libraries.places.api.net.PlacesClient
import com.hypertrack.android.interactors.GooglePlacesInteractor
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.ui.common.select_destination.SelectDestinationViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.history.DeviceLocationProvider
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.OsUtilsProvider


class AddOrderViewModel(
    private val tripId: String,
    baseDependencies: BaseViewModelDependencies,
    private val placesInteractor: PlacesInteractor,
    private val googlePlacesInteractor: GooglePlacesInteractor,
    private val deviceLocationProvider: DeviceLocationProvider,
) : SelectDestinationViewModel(
    baseDependencies,
    placesInteractor,
    googlePlacesInteractor,
    deviceLocationProvider,
) {
    override fun proceed(destinationData: DestinationData) {
        destination.postValue(
            AddOrderFragmentDirections.actionAddOrderFragmentToAddOrderInfoFragment(
                destinationData, tripId
            )
        )
    }
}