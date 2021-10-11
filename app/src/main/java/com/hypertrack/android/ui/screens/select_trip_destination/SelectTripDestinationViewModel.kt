package com.hypertrack.android.ui.screens.select_trip_destination

import com.google.android.libraries.places.api.net.PlacesClient
import com.hypertrack.android.interactors.GooglePlacesInteractor
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.ui.common.select_destination.SelectDestinationViewModel
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.DeviceLocationProvider
import com.hypertrack.android.utils.OsUtilsProvider

class SelectTripDestinationViewModel(
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
            SelectTripDestinationFragmentDirections
                .actionSelectTripDestinationFragmentToAddOrderInfoFragment(
                    destinationData,
                    tripId = null
                )
        )
    }
}