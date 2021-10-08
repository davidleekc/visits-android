package com.hypertrack.android.ui.screens.confirm_email

import android.app.Activity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hypertrack.android.interactors.*
import com.hypertrack.android.ui.base.*
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.launch

class ConfirmEmailViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val loginInteractor: LoginInteractor,
    private val permissionsInteractor: PermissionsInteractor,
) : BaseViewModel(baseDependencies) {

    private lateinit var email: String

    val proceedButtonEnabled = MutableLiveData<Boolean>(false)
    val clipboardCode = SingleLiveEvent<String>()

    fun init(email: String) {
        this.email = email
    }

    fun onClipboardReady() {
        osUtilsProvider.getClipboardContents()?.let {
            if (it.matches(Regex("^[0-9]{6}\$"))) {
                clipboardCode.postValue(it)
            }
        }
    }

    fun onVerifiedClick(code: String, complete: Boolean, activity: Activity) {
        if (complete) {
            loadingState.postValue(true)
            viewModelScope.launch {
                val res = loginInteractor.verifyByOtpCode(email = email, code = code)
                loadingState.postValue(false)
                when (res) {
                    is OtpSuccess -> {
                        when (permissionsInteractor.checkPermissionsState()
                            .getNextPermissionRequest()) {
                            PermissionDestination.PASS -> {
                                destination.postValue(ConfirmFragmentDirections.actionGlobalVisitManagementFragment())
                            }
                            PermissionDestination.FOREGROUND_AND_TRACKING -> {
                                destination.postValue(ConfirmFragmentDirections.actionGlobalPermissionRequestFragment())
                            }
                            PermissionDestination.BACKGROUND -> {
                                destination.postValue(ConfirmFragmentDirections.actionGlobalBackgroundPermissionsFragment())
                            }
                        }
                    }
                    is OtpSignInRequired -> {
                        destination.postValue(
                            ConfirmFragmentDirections.actionConfirmFragmentToSignInFragment(
                                email
                            )
                        )
                    }
                    is OtpWrongCode -> {
                        errorHandler.postText(R.string.wrong_code)
                    }
                    is OtpError -> {
                        errorHandler.postException(res.exception)
                    }
                }
            }
        }
    }

    fun onResendClick() {
        loadingState.postValue(true)
        viewModelScope.launch {
            val res = loginInteractor.resendEmailConfirmation(email)
            loadingState.postValue(false)
            when (res) {
                ResendNoAction -> {
                    return@launch
                }
                ResendAlreadyConfirmed -> {
                    destination.postValue(
                        ConfirmFragmentDirections.actionConfirmFragmentToSignInFragment(
                            email
                        )
                    )
                }
                is ResendError -> {
                    errorHandler.postException(res.exception)
                }
            }

        }
    }

    fun onCodeChanged(code: String, complete: Boolean, activity: Activity) {
        if (complete) {
            onVerifiedClick(code, complete, activity)
        }
        proceedButtonEnabled.postValue(complete)
    }

}