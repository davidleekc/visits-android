package com.hypertrack.android.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hypertrack.android.api.ApiClient
import com.hypertrack.android.interactors.PermissionsInteractor
import com.hypertrack.android.repository.*
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.profile.ProfileViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.summary.SummaryViewModel
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.HyperTrackService
import com.hypertrack.android.ui.screens.driver_id_input.DriverLoginViewModel
import com.hypertrack.android.ui.screens.permission_request.PermissionRequestViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.places.PlacesViewModel
import com.hypertrack.android.utils.TimeDistanceFormatter
import com.hypertrack.android.view_models.HistoryViewModel

@Suppress("UNCHECKED_CAST")
class UserScopeViewModelFactory(
    private val visitsRepository: VisitsRepository,
    private val placesRepository: PlacesRepository,
    private val historyRepository: HistoryRepository,
    private val driverRepository: DriverRepository,
    private val accountRepository: AccountRepository,
    private val crashReportsProvider: CrashReportsProvider,
    private val hyperTrackService: HyperTrackService,
    private val permissionsInteractor: PermissionsInteractor,
    private val accessTokenRepository: AccessTokenRepository,
    private val timeLengthFormatter: TimeDistanceFormatter,
    private val apiClient: ApiClient
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            PlacesViewModel::class.java -> PlacesViewModel(
                placesRepository
            ) as T
            PermissionRequestViewModel::class.java -> PermissionRequestViewModel(
                permissionsInteractor,
                hyperTrackService
            ) as T
            SummaryViewModel::class.java -> SummaryViewModel(historyRepository) as T
            HistoryViewModel::class.java -> HistoryViewModel(
                historyRepository,
                timeLengthFormatter
            ) as T
            DriverLoginViewModel::class.java -> DriverLoginViewModel(
                driverRepository,
                hyperTrackService,
                permissionsInteractor
            ) as T
            VisitsManagementViewModel::class.java -> VisitsManagementViewModel(
                visitsRepository,
                historyRepository,
                accountRepository,
                crashReportsProvider,
                accessTokenRepository
            ) as T
            ProfileViewModel::class.java -> ProfileViewModel(
                driverRepository,
                hyperTrackService
            ) as T
            else -> throw IllegalArgumentException("Can't instantiate class $modelClass")
        }
    }
}