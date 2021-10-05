package com.hypertrack.android.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.libraries.places.api.net.PlacesClient
import com.hypertrack.android.api.ApiClient
import com.hypertrack.android.interactors.FeedbackInteractor
import com.hypertrack.android.interactors.PermissionsInteractor
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.interactors.TripsInteractor
import com.hypertrack.android.repository.*
import com.hypertrack.android.ui.screens.add_integration.AddIntegrationViewModel
import com.hypertrack.android.ui.screens.add_place.AddPlaceViewModel
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.profile.ProfileViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.summary.SummaryViewModel
import com.hypertrack.android.ui.screens.permission_request.PermissionRequestViewModel
import com.hypertrack.android.ui.common.select_destination.SelectDestinationViewModel
import com.hypertrack.android.ui.screens.select_trip_destination.SelectTripDestinationViewModel
import com.hypertrack.android.ui.screens.send_feedback.SendFeedbackViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.current_trip.CurrentTripViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.history.DeviceLocationProvider
import com.hypertrack.android.ui.screens.visits_management.tabs.orders.OrdersListViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.places.PlacesViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.places.PlacesVisitsViewModel
import com.hypertrack.android.utils.*
import com.hypertrack.android.ui.screens.visits_management.tabs.history.HistoryViewModel
import com.hypertrack.android.utils.formatters.DatetimeFormatter
import com.hypertrack.android.utils.formatters.DistanceFormatter
import com.hypertrack.android.utils.formatters.TimeFormatter
import javax.inject.Provider

@Suppress("UNCHECKED_CAST")
class UserScopeViewModelFactory(
    private val appScope: AppScope,
    private val userScopeProvider: Provider<UserScope>,
    private val tripsInteractor: TripsInteractor,
    private val placesInteractor: PlacesInteractor,
    private val feedbackInteractor: FeedbackInteractor,
    private val integrationsRepository: IntegrationsRepository,
    private val driverRepository: DriverRepository,
    private val accountRepository: AccountRepository,
    private val crashReportsProvider: CrashReportsProvider,
    private val hyperTrackService: HyperTrackService,
    private val permissionsInteractor: PermissionsInteractor,
    private val accessTokenRepository: AccessTokenRepository,
    private val apiClient: ApiClient,
    private val osUtilsProvider: OsUtilsProvider,
    private val placesClient: PlacesClient,
    private val deviceLocationProvider: DeviceLocationProvider,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            SendFeedbackViewModel::class.java -> SendFeedbackViewModel(
                feedbackInteractor,
                osUtilsProvider
            ) as T
            OrdersListViewModel::class.java -> OrdersListViewModel(
                tripsInteractor,
                userScopeProvider.get().tripsUpdateTimerInteractor,
                appScope.datetimeFormatter,
                osUtilsProvider
            ) as T
            AddIntegrationViewModel::class.java -> AddIntegrationViewModel(
                integrationsRepository
            ) as T
            AddPlaceViewModel::class.java -> AddPlaceViewModel(
                userScopeProvider.get().placesInteractor,
                userScopeProvider.get().googlePlacesInteractor,
                osUtilsProvider,
                deviceLocationProvider,
                crashReportsProvider
            ) as T
            CurrentTripViewModel::class.java -> CurrentTripViewModel(
                tripsInteractor,
                placesInteractor,
                userScopeProvider.get().tripsUpdateTimerInteractor,
                hyperTrackService,
                deviceLocationProvider,
                osUtilsProvider,
                crashReportsProvider,
                appScope.datetimeFormatter,
                appScope.timeFormatter
            ) as T
            SelectDestinationViewModel::class.java -> SelectDestinationViewModel(
                userScopeProvider.get().placesInteractor,
                userScopeProvider.get().googlePlacesInteractor,
                osUtilsProvider,
                deviceLocationProvider,
                crashReportsProvider
            ) as T
            PlacesViewModel::class.java -> PlacesViewModel(
                userScopeProvider.get().placesInteractor,
                osUtilsProvider,
                deviceLocationProvider,
                appScope.distanceFormatter,
                appScope.datetimeFormatter
            ) as T
            PermissionRequestViewModel::class.java -> PermissionRequestViewModel(
                permissionsInteractor,
                hyperTrackService
            ) as T
            SummaryViewModel::class.java -> SummaryViewModel(
                userScopeProvider.get().historyInteractor,
                osUtilsProvider,
                appScope.distanceFormatter,
                appScope.timeFormatter
            ) as T
            HistoryViewModel::class.java -> HistoryViewModel(
                userScopeProvider.get().historyInteractor,
                appScope.datetimeFormatter,
                appScope.distanceFormatter,
                osUtilsProvider
            ) as T
            VisitsManagementViewModel::class.java -> VisitsManagementViewModel(
                userScopeProvider.get().historyInteractor,
                accountRepository,
                hyperTrackService,
                crashReportsProvider,
                osUtilsProvider,
                accessTokenRepository
            ) as T
            ProfileViewModel::class.java -> ProfileViewModel(
                driverRepository,
                hyperTrackService,
                accountRepository,
                osUtilsProvider,
                crashReportsProvider,
            ) as T
            SelectTripDestinationViewModel::class.java -> SelectTripDestinationViewModel(
                userScopeProvider.get().placesInteractor,
                userScopeProvider.get().googlePlacesInteractor,
                deviceLocationProvider,
                osUtilsProvider,
                crashReportsProvider
            ) as T
            PlacesVisitsViewModel::class.java -> PlacesVisitsViewModel(
                userScopeProvider.get().placesVisitsInteractor,
                userScopeProvider.get().historyInteractor,
                osUtilsProvider,
                appScope.datetimeFormatter,
                appScope.distanceFormatter,
                appScope.timeFormatter,
            ) as T
            else -> throw IllegalArgumentException("Can't instantiate class $modelClass")
        }
    }
}