package com.hypertrack.android.ui.screens.visits_management

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.*
import com.hypertrack.android.interactors.HistoryInteractor
import com.hypertrack.android.repository.AccessTokenRepository
import com.hypertrack.android.repository.AccountRepository
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.ErrorHandler
import com.hypertrack.android.utils.*
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@SuppressLint("NullSafeMutableLiveData")
class VisitsManagementViewModel(
    private val historyInteractor: HistoryInteractor,
    private val accountRepository: AccountRepository,
    private val hyperTrackService: HyperTrackService,
    private val crashReportsProvider: CrashReportsProvider,
    private val osUtilsProvider: OsUtilsProvider,
    accessTokenRepository: AccessTokenRepository
) : BaseViewModel(osUtilsProvider) {

    override val errorHandler =
        ErrorHandler(osUtilsProvider, historyInteractor.errorFlow.asLiveData())

    val isTracking = Transformations.map(hyperTrackService.state) {
        it == TrackingStateValue.TRACKING
    }

    init {
        if (accountRepository.shouldStartTracking) {
            hyperTrackService.startTracking()
            accountRepository.shouldStartTracking = false
        }
    }

    val deviceHistoryWebUrl = accessTokenRepository.deviceHistoryWebViewUrl

    private val _showSpinner = MutableLiveData(false)
    val showSpinner: LiveData<Boolean>
        get() = _showSpinner

    private val _showSync = MutableLiveData(false)
    val showSync: LiveData<Boolean>
        get() = _showSync

    private val _showToast = MutableLiveData("")
    val showToast: LiveData<String>
        get() = _showToast

    private val _statusBarColor = MediatorLiveData<Int?>()

    init {
        _statusBarColor.addSource(hyperTrackService.state) {
            when (it) {
                TrackingStateValue.TRACKING -> _statusBarColor.postValue(R.color.colorTrackingActive)
                TrackingStateValue.STOP -> _statusBarColor.postValue(R.color.colorTrackingStopped)
                TrackingStateValue.DEVICE_DELETED,
                TrackingStateValue.ERROR,
                TrackingStateValue.PERMISIONS_DENIED -> {
                    _statusBarColor.postValue(
                        R.color.colorTrackingError
                    )
                }
                else -> _statusBarColor.postValue(null)
            }
        }
    }

    val statusBarColor: LiveData<Int?>
        get() = _statusBarColor

    val statusBarMessage = MediatorLiveData<StatusMessage>()

    init {
        statusBarMessage.addSource(hyperTrackService.state) {
            statusBarMessage.postValue(
                when (it) {
                    TrackingStateValue.DEVICE_DELETED -> StatusString(R.string.device_deleted)
                    TrackingStateValue.ERROR -> StatusString(R.string.generic_tracking_error)
                    TrackingStateValue.TRACKING -> StatusString(R.string.clocked_in)
                    TrackingStateValue.STOP -> StatusString(R.string.clocked_out)
                    TrackingStateValue.PERMISIONS_DENIED -> StatusString(R.string.permissions_not_granted)
                    else -> StatusString(R.string.unknown_error)
                }
            )
        }
    }

    fun refreshHistory() {
        MainScope().launch {
            historyInteractor.loadTodayHistory()
        }
    }

    fun switchTracking() {
        _showSpinner.postValue(true)
        viewModelScope.launch {
            if (isTracking.value == true) {
                hyperTrackService.stopTracking()
            } else {
                hyperTrackService.startTracking()
            }
            _showSpinner.postValue(false)
        }
    }

    companion object {
        const val TAG = "VisitsManagementVM"
    }

}

sealed class StatusMessage
class StatusString(val stringId: Int) : StatusMessage()

enum class LocalVisitCtaLabel {
    CHECK_IN, CHECK_OUT
}