package com.hypertrack.android.ui.screens.splash_screen

import android.app.Activity
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.hypertrack.android.interactors.PermissionDestination
import com.hypertrack.android.interactors.PermissionsInteractor
import com.hypertrack.android.repository.AccountRepository
import com.hypertrack.android.repository.DriverRepository
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.common.isEmail
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.R
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.launch

class SplashScreenViewModel(
    private val driverRepository: DriverRepository,
    private val accountRepository: AccountRepository,
    private val crashReportsProvider: CrashReportsProvider,
    private val permissionsInteractor: PermissionsInteractor,
    private val osUtilsProvider: OsUtilsProvider,
    private val moshi: Moshi
) : BaseViewModel() {

    val loadingState = MutableLiveData<Boolean>()

    fun handleDeeplink(parameters: Map<String, Any>, activity: Activity) {
//        Log.v("hypertrack-verbose", parameters.toString())
        if (parameters.isNotEmpty()) {
            val key = parameters["publishable_key"] as String?
            val driverId = parameters["driver_id"] as String?
            val email = parameters["email"] as String?
            val deeplink = parameters["~referring_link"] as String?
            val deeplinkWithoutGetParams = deeplink.urlClearGetParams()
            val phoneNumber = parameters["phone_number"] as String?
            val metadata: Map<String, Any>? = try {
                val param = parameters["metadata"]
                when (param) {
                    is String -> {
                        moshi.adapter<Map<String, Any>>(
                            Types.newParameterizedType(
                                Map::class.java, String::class.java,
                                Any::class.java
                            )
                        ).fromJson(param)
                    }
                    else -> null
                }
            } catch (e: Exception) {
                crashReportsProvider.logException(e)
                errorHandler.postText(
                    osUtilsProvider.stringFromResource(
                        R.string.splash_screen_invalid_link,
                        osUtilsProvider.stringFromResource(
                            R.string.splash_screen_wrong_metadata,
                            parameters["metadata"].toString()
                        )
                    )
                )
                proceedToSignUp()
                return
            }

            when {
                key == null -> {
                    errorHandler.postText(
                        osUtilsProvider.stringFromResource(
                            R.string.splash_screen_invalid_link,
                            osUtilsProvider.stringFromResource(R.string.splash_screen_no_key)
                        )
                    )
                    proceedToSignUp()
                }
                email == null && phoneNumber == null && driverId == null -> {
                    errorHandler.postText(
                        osUtilsProvider.stringFromResource(
                            R.string.splash_screen_invalid_link,
                            osUtilsProvider.stringFromResource(R.string.splash_screen_no_username)
                        )
                    )
                    proceedToSignUp()
                }
                else -> {
                    loadingState.postValue(true)
                    viewModelScope.launch {
                        try {
                            val correctKey = accountRepository.onKeyReceived(key)
                            // Log.d(TAG, "onKeyReceived finished")
                            if (correctKey) {
                                // Log.d(TAG, "Key validated successfully")
                                if (driverId != null && (email == null && phoneNumber == null)) {
                                    errorHandler.postText(
                                        osUtilsProvider.stringFromResource(
                                            R.string.splash_screen_deprecated_link
                                        )
                                    )
                                    if (driverId.isEmail()) {
                                        driverRepository.setUserData(
                                            email = driverId,
                                            phoneNumber = phoneNumber,
                                            metadata = metadata,
                                            deeplinkWithoutGetParams = deeplinkWithoutGetParams
                                        )
                                    } else {
                                        driverRepository.setUserData(
                                            driverId = driverId,
                                            phoneNumber = phoneNumber,
                                            metadata = metadata,
                                            deeplinkWithoutGetParams = deeplinkWithoutGetParams
                                        )
                                    }
                                    proceedToVisitsManagement(activity)
                                } else {
                                    driverRepository.setUserData(
                                        email = email,
                                        phoneNumber = phoneNumber,
                                        metadata = metadata,
                                        deeplinkWithoutGetParams = deeplinkWithoutGetParams
                                    )
                                    proceedToVisitsManagement(activity)
                                }
                            } else {
                                throw Exception("Invalid publishable_key")
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Cannot validate the key", e)
                            errorHandler.postException(e)
                            proceedToSignUp()
                        }
                    }
                }
            }
        } else {
            if (accountRepository.isVerifiedAccount) {
                proceedToVisitsManagement(activity)
            } else {
                proceedToSignUp()
            }
        }
    }

    private fun proceedToSignUp() {
        loadingState.postValue(false)
        destination.postValue(
            SplashScreenFragmentDirections.actionSplashScreenFragmentToSignUpFragment()
        )
    }

    private fun proceedToVisitsManagement(activity: Activity) {
        loadingState.postValue(false)
        when (permissionsInteractor.checkPermissionsState()
            .getNextPermissionRequest()) {
            PermissionDestination.PASS -> {
                destination.postValue(SplashScreenFragmentDirections.actionGlobalVisitManagementFragment())
            }
            PermissionDestination.FOREGROUND_AND_TRACKING -> {
                destination.postValue(SplashScreenFragmentDirections.actionGlobalPermissionRequestFragment())
            }
            PermissionDestination.BACKGROUND -> {
                destination.postValue(SplashScreenFragmentDirections.actionGlobalBackgroundPermissionsFragment())
            }
        }
    }

    companion object {
        const val TAG = "SplashScreenVM"
    }

    fun String?.toBoolean(): Boolean? {
        return when (this) {
            "False", "false" -> false
            "true", "True" -> true
            "", null -> null
            else -> null
        }
    }

    fun String?.urlClearGetParams(): String? {
        return this?.split("?")?.first()
    }
}
