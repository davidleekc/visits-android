package com.hypertrack.android.ui.screens.visits_management.tabs.profile

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hypertrack.android.repository.AccountRepository
import com.hypertrack.android.repository.DriverRepository
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.common.KeyValueItem
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragment
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragmentDirections
import com.hypertrack.android.utils.*
import com.hypertrack.logistics.android.github.BuildConfig
import com.hypertrack.logistics.android.github.R

class ProfileViewModel(
    driverRepository: DriverRepository,
    hyperTrackService: HyperTrackService,
    accountRepository: AccountRepository,
    private val osUtilsProvider: OsUtilsProvider,
    private val crashReportsProvider: CrashReportsProvider,
) : BaseViewModel() {

    val profile = MutableLiveData<List<KeyValueItem>>(mutableListOf<KeyValueItem>().apply {
        driverRepository.user?.let { user ->
            user.email?.let {
                add(
                    KeyValueItem(
                        osUtilsProvider.stringFromResource(R.string.email),
                        it
                    )
                )
            }
            user.phoneNumber?.let {
                add(
                    KeyValueItem(
                        osUtilsProvider.stringFromResource(R.string.phone_number),
                        it
                    )
                )
            }
            user.driverId?.let {
                add(
                    KeyValueItem(
                        osUtilsProvider.stringFromResource(R.string.driver_id),
                        it
                    )
                )
            }
        }

        add(
            KeyValueItem(
                osUtilsProvider.stringFromResource(R.string.device_id),
                hyperTrackService.deviceId ?: ""
            )
        )
        if (BuildConfig.DEBUG) {
            add(
                KeyValueItem(
                    "Publishable key (debug)",
                    accountRepository.publishableKey
                )
            )
        }
        osUtilsProvider.getBuildVersion()?.let {
            add(
                KeyValueItem(
                    osUtilsProvider.stringFromResource(R.string.app_version),
                    it
                )
            )
        }
    })

    fun onCopyItemClick(txt: String) {
        osUtilsProvider.copyToClipboard(txt)
    }

    fun onReportAnIssueClick() {
        destination.postValue(VisitsManagementFragmentDirections.actionVisitManagementFragmentToSendFeedbackFragment())
    }

}