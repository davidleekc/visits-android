package com.hypertrack.android.ui.screens.splash_screen

import android.app.Activity
import androidx.lifecycle.viewModelScope
import com.hypertrack.android.interactors.PermissionDestination
import com.hypertrack.android.interactors.PermissionsInteractor
import com.hypertrack.android.repository.AccountRepository
import com.hypertrack.android.repository.DriverRepository
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.base.postValue
import com.hypertrack.android.utils.format
import com.hypertrack.logistics.android.github.R
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.launch

class SplashScreenViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val driverRepository: DriverRepository,
    private val accountRepository: AccountRepository,
    private val permissionsInteractor: PermissionsInteractor,
    private val moshi: Moshi
) : BaseViewModel(baseDependencies) {

    fun handleDeeplink(parameters: Map<String, Any>, activity: Activity) {
//        Log.v("hypertrack-verbose", parameters.toString())
        if (parameters.isNotEmpty()) {
            crashReportsProvider.log("Got deeplink: ${parameters}")
            try {
                parseDeeplink(parameters, activity)
            } catch (e: Exception) {
                errorHandler.postText(
                    osUtilsProvider.stringFromResource(
                        R.string.splash_screen_invalid_link,
                        e.format()
                    )
                )
                crashReportsProvider.logException(InvalidDeeplinkException(e))
                onDeeplinkError(activity)
            }
        } else {
            proceedWithLoggedInCheck(activity)
        }
    }

    private fun parseDeeplink(parameters: Map<String, Any>, activity: Activity) {
        val publishableKey = parameters["publishable_key"] as String?
        val driverId = parameters["driver_id"] as String?
        val email = parameters["email"] as String?
        val deeplink = parameters["~referring_link"] as String?
        val deeplinkWithoutGetParams = deeplink.urlClearGetParams()
        val phoneNumber = parameters["phone_number"] as String?
        val metadata: Map<String, Any>? = when (val param = parameters["metadata"]) {
            is String -> {
                moshi.adapter<Map<String, Any>>(
                    Types.newParameterizedType(
                        Map::class.java, String::class.java,
                        Any::class.java
                    )
                ).fromJson(param)
            }
            is Map<*, *> -> param as Map<String, Any>
            else -> null
        }

        when {
            publishableKey == null -> {
                errorHandler.postText(
                    osUtilsProvider.stringFromResource(
                        R.string.splash_screen_invalid_link,
                        osUtilsProvider.stringFromResource(R.string.splash_screen_no_key)
                    )
                )
                crashReportsProvider.logException(InvalidDeeplinkException("publishableKey == null"))
                onDeeplinkError(activity)
            }
            email == null && phoneNumber == null && driverId == null -> {
                errorHandler.postText(
                    osUtilsProvider.stringFromResource(
                        R.string.splash_screen_invalid_link,
                        osUtilsProvider.stringFromResource(R.string.splash_screen_no_username)
                    )
                )
                crashReportsProvider.logException(
                    InvalidDeeplinkException(
                        "email == null && phoneNumber == null && driverId == null"
                    )
                )
                onDeeplinkError(activity)
            }
            metadata?.containsKey("email") == true && email != null ||
                    metadata?.containsKey("phone_number") == true && phoneNumber != null -> {
                errorHandler.postText(
                    osUtilsProvider.stringFromResource(
                        R.string.splash_screen_invalid_link,
                        osUtilsProvider.stringFromResource(R.string.splash_screen_duplicate_fields)
                    )
                )
                crashReportsProvider.logException(
                    InvalidDeeplinkException(
                        "metadata?.containsKey(\"email\") == true && email != null ||\n" +
                                "            metadata?.containsKey(\"phone_number\") == true && phoneNumber != null"
                    )
                )
                onDeeplinkError(activity)
            }
            else -> {
                validatePublishableKeyAndProceed(
                    publishableKey = publishableKey,
                    driverId = driverId,
                    email = email,
                    phoneNumber = phoneNumber,
                    metadata = metadata,
                    deeplinkWithoutGetParams = deeplinkWithoutGetParams,
                    activity = activity
                )
            }
        }
    }

    private fun validatePublishableKeyAndProceed(
        publishableKey: String,
        driverId: String?,
        email: String?,
        phoneNumber: String?,
        metadata: Map<String, Any>?,
        deeplinkWithoutGetParams: String?,
        activity: Activity
    ) {
        loadingState.postValue(true)
        viewModelScope.launch {
            try {
                val correctKey = accountRepository.onKeyReceived(publishableKey)
                // Log.d(TAG, "onKeyReceived finished")
                if (correctKey) {
                    // Log.d(TAG, "Key validated successfully")
                    if (driverId != null && (email == null && phoneNumber == null)) {
                        driverRepository.setUserData(
                            driverId = driverId,
                            metadata = metadata,
                            deeplinkWithoutGetParams = deeplinkWithoutGetParams
                        )
                        proceedToVisitsManagement(activity)
                    } else {
                        driverRepository.setUserData(
                            email = email,
                            phoneNumber = phoneNumber,
                            //ignored, because this field is sent only as fallback for older versions of Visits app
                            //email is used instead
                            //driverId = driverId,
                            metadata = metadata,
                            deeplinkWithoutGetParams = deeplinkWithoutGetParams
                        )
                        proceedToVisitsManagement(activity)
                    }
                } else {
                    throw Exception("Invalid publishable_key")
                }
            } catch (e: Exception) {
                errorHandler.postException(e)
                crashReportsProvider.logException(InvalidDeeplinkException(e))
                onDeeplinkError(activity)
            }
        }
    }

    private fun onDeeplinkError(activity: Activity) {
        proceedWithLoggedInCheck(activity)
    }

    private fun proceedWithLoggedInCheck(activity: Activity) {
        if (accountRepository.isVerifiedAccount) {
            proceedToVisitsManagement(activity)
        } else {
            proceedToSignIn()
        }
    }

    private fun proceedToSignIn() {
        loadingState.postValue(false)
        destination.postValue(
            SplashScreenFragmentDirections.actionSplashScreenFragmentToSignInFragment()
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

    class InvalidDeeplinkException(m: String) : Exception(m) {
        constructor(e: Exception) : this(e.format())
    }
}


