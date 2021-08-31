package com.hypertrack.android.ui.screens.sign_up

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.amazonaws.AmazonServiceException
import com.hypertrack.android.interactors.ConfirmationRequired
import com.hypertrack.android.interactors.LoginInteractor
import com.hypertrack.android.interactors.SignUpError
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.ui.common.util.isEmail
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.launch

class SignUpViewModel(
    private val loginInteractor: LoginInteractor,
    private val osUtilsProvider: OsUtilsProvider
) : BaseViewModel() {

    val errorTextState = MutableLiveData<String?>()
    val page = MutableLiveData<Int>(0)

    fun onSignUpClicked(login: String, password: String, userAttributes: Map<String, String>) {
        when {
            password.length < 8 -> {
                page.postValue(SignUpFragment.PAGE_USER)
                errorTextState.postValue(osUtilsProvider.stringFromResource(R.string.password_too_short))
            }
            !login.isEmail() -> {
                page.postValue(SignUpFragment.PAGE_USER)
                errorTextState.postValue(osUtilsProvider.stringFromResource(R.string.invalid_email))
            }
            else -> {
                viewModelScope.launch {
                    val res = loginInteractor.signUp(login, password, userAttributes)
                    when (res) {
                        ConfirmationRequired -> {
                            throw NotImplementedError()
//                            destination.postValue(
//                                SignUpFragmentDirections.actionSignUpFragmentToConfirmFragment(
//                                    login
//                                )
//                            )
                        }
                        is SignUpError -> {
                            when (res.exception) {
                                is AmazonServiceException -> {
                                    errorTextState.postValue(res.exception.errorMessage)
                                }
                                else -> {
                                    errorTextState.postValue(res.exception.message)
                                }
                            }

                        }
                    }
                }
            }
        }
    }

    fun onSignInClicked() {
        throw NotImplementedError()
//        destination.postValue(SignUpFragmentDirections.actionSignUpFragmentToSignInFragment(null))
    }

    fun onBackPressed(): Boolean {
        if (page.value == SignUpFragment.PAGE_INFO) {
            page.postValue(SignUpFragment.PAGE_USER)
            return true
        } else {
            return false
        }
    }

    fun onNextClicked(email: String, password: String) {
        if (email.isEmail()) {
            page.postValue(SignUpFragment.PAGE_INFO)
        } else {
            page.postValue(SignUpFragment.PAGE_USER)
            errorTextState.postValue(osUtilsProvider.stringFromResource(R.string.invalid_email))
        }
    }

}