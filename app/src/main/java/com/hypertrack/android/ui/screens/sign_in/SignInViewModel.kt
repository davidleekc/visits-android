package com.hypertrack.android.ui.screens.sign_in

import android.app.Activity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hypertrack.android.interactors.*
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.base.postValue
import com.hypertrack.android.utils.*
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.launch

class SignInViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val loginInteractor: LoginInteractor,
    private val permissionsInteractor: PermissionsInteractor,
) : BaseViewModel(baseDependencies) {

    private var login = ""
    private var password = ""

    val errorTextState = MutableLiveData<String>()
    val showProgress = MutableLiveData(false)
    val isLoginButtonClickable = MutableLiveData(false)

    fun onLoginTextChanged(email: CharSequence) {
        // Log.v(TAG, "onLoginTextChanged $email")
        login = email.toString()
        enableButtonIfInputNonEmpty()
    }

    fun onPasswordTextChanged(pwd: CharSequence) {
        // Log.v(TAG, "onPasswordTextChanged $pwd")
        password = pwd.toString()
        enableButtonIfInputNonEmpty()
    }

    fun onLoginClick(activity: Activity) {
        errorTextState.postValue("")
        // Log.v(TAG, "onLoginClick")
        isLoginButtonClickable.postValue(false)
        showProgress.postValue(true)

        viewModelScope.launch {
            val res = loginInteractor.signIn(login, password)
            when (res) {
                is PublishableKey -> {
                    showProgress.postValue(false)
                    proceed(activity)
                }
                else -> {
                    enableButtonIfInputNonEmpty()
                    showProgress.postValue(false)
                    when (res) {
                        is NoSuchUser -> {
                            errorTextState.postValue(osUtilsProvider.stringFromResource(R.string.user_does_not_exist))
                        }
                        is InvalidLoginOrPassword -> {
                            errorTextState.postValue(osUtilsProvider.stringFromResource(R.string.incorrect_username_or_pass))
                        }
                        is EmailConfirmationRequired -> {
                            destination.postValue(
                                SignInFragmentDirections.actionSignInFragmentToConfirmFragment(
                                    login
                                )
                            )
                        }
                        is LoginError -> {
                            errorTextState.postValue(MyApplication.context.getString(R.string.unknown_error))
                        }
                        is PublishableKey -> throw IllegalStateException()
                    }
                }
            }
        }
    }

    private fun proceed(activity: Activity) {
        when (permissionsInteractor.checkPermissionsState().getNextPermissionRequest()) {
            PermissionDestination.PASS -> {
                destination.postValue(SignInFragmentDirections.actionGlobalVisitManagementFragment())
            }
            PermissionDestination.FOREGROUND_AND_TRACKING -> {
                destination.postValue(SignInFragmentDirections.actionGlobalPermissionRequestFragment())
            }
            PermissionDestination.BACKGROUND -> {
                destination.postValue(SignInFragmentDirections.actionGlobalBackgroundPermissionsFragment())
            }
        }
    }

    private fun enableButtonIfInputNonEmpty() {
        // Log.v(TAG, "enableButtonIfInputNonEmpty")
        if (login.isNotBlank() && password.isNotBlank()) {
            // Log.v(TAG, "enabling Button")
            isLoginButtonClickable.postValue(true)
        } else {
            isLoginButtonClickable.postValue(false)
        }
    }

//    fun onSignUpClick() {
//        destination.postConsumable(SignInFragmentDirections.actionSignInFragmentToSignUpFragment())
//    }

    fun Int.stringFromResource(): String {
        return MyApplication.context.getString(this)
    }

    companion object {
        const val TAG = "AccountLoginVM"
    }
}