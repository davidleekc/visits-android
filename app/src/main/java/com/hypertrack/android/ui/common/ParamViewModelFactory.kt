package com.hypertrack.android.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.libraries.places.api.net.PlacesClient
import com.hypertrack.android.api.ApiClient
import com.hypertrack.android.repository.AccountRepository
import com.hypertrack.android.ui.screens.add_order.AddOrderViewModel
import com.hypertrack.android.ui.screens.add_order_info.AddOrderInfoViewModel
import com.hypertrack.android.ui.screens.add_place_info.AddPlaceInfoViewModel
import com.hypertrack.android.ui.screens.place_details.PlaceDetailsViewModel
import com.hypertrack.android.ui.screens.order_details.OrderDetailsViewModel
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.ui.screens.select_trip_destination.SelectTripDestinationViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.history.DeviceLocationProvider
import com.hypertrack.android.utils.*
import com.squareup.moshi.Moshi
import kotlinx.coroutines.GlobalScope
import javax.inject.Provider

@Suppress("UNCHECKED_CAST")
class ParamViewModelFactory<T>(
    private val param: T,
    private val userScopeProvider: Provider<UserScope>,
    private val osUtilsProvider: OsUtilsProvider,
    private val accountRepository: AccountRepository,
    private val apiClient: ApiClient,
    private val moshi: Moshi,
    private val crashReportsProvider: CrashReportsProvider,
    private val placesClient: PlacesClient,
    private val deviceLocationProvider: DeviceLocationProvider,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            PlaceDetailsViewModel::class.java -> PlaceDetailsViewModel(
                geofenceId = param as String,
                userScopeProvider.get().placesInteractor,
                osUtilsProvider,
                crashReportsProvider,
                moshi
            ) as T
            OrderDetailsViewModel::class.java -> OrderDetailsViewModel(
                orderId = param as String,
                userScopeProvider.get().tripsInteractor,
                userScopeProvider.get().photoUploadQueueInteractor,
                osUtilsProvider,
                accountRepository,
                apiClient,
                GlobalScope
            ) as T
            AddPlaceInfoViewModel::class.java -> (param as DestinationData).let { destinationData ->
                AddPlaceInfoViewModel(
                    destinationData.latLng,
                    initialAddress = destinationData.address,
                    _name = destinationData.name,
                    userScopeProvider.get().placesInteractor,
                    userScopeProvider.get().integrationsRepository,
                    osUtilsProvider,
                    moshi
                ) as T
            }
            AddOrderInfoViewModel::class.java -> AddOrderInfoViewModel(
                param as AddOrderInfoViewModel.Params,
                userScopeProvider.get().tripsInteractor,
                osUtilsProvider
            ) as T
            AddOrderViewModel::class.java -> AddOrderViewModel(
                param as String,
                userScopeProvider.get().placesInteractor,
                placesClient,
                deviceLocationProvider,
                osUtilsProvider,
            ) as T
            else -> throw IllegalArgumentException("Can't instantiate class $modelClass")
        }
    }
}