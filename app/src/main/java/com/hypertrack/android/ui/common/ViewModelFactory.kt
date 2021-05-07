package com.hypertrack.android.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hypertrack.android.interactors.LoginInteractor
import com.hypertrack.android.interactors.PermissionsInteractor
import com.hypertrack.android.repository.AccountRepository
import com.hypertrack.android.repository.DriverRepository
import com.hypertrack.android.ui.screens.background_permissions.BackgroundPermissionsViewModel
import com.hypertrack.android.ui.screens.confirm_email.ConfirmEmailViewModel
import com.hypertrack.android.ui.screens.sign_in.SignInViewModel
import com.hypertrack.android.ui.screens.sign_up.SignUpViewModel
import com.hypertrack.android.ui.screens.splash_screen.SplashScreenViewModel
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.OsUtilsProvider

@Suppress("UNCHECKED_CAST")
class ViewModelFactory(
    private val accountRepository: AccountRepository,
    private val driverRepository: DriverRepository,
    private val crashReportsProvider: CrashReportsProvider,
    private val permissionsInteractor: PermissionsInteractor,
    private val loginInteractor: LoginInteractor,
    private val osUtilsProvider: OsUtilsProvider,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            ConfirmEmailViewModel::class.java -> ConfirmEmailViewModel(
                loginInteractor,
                permissionsInteractor,
                osUtilsProvider
            ) as T
            SignInViewModel::class.java -> SignInViewModel(
                loginInteractor,
                permissionsInteractor,
                osUtilsProvider
            ) as T
            SignUpViewModel::class.java -> SignUpViewModel(
                loginInteractor,
                osUtilsProvider,
            ) as T
            SplashScreenViewModel::class.java -> SplashScreenViewModel(
                driverRepository,
                accountRepository,
                crashReportsProvider,
                permissionsInteractor
            ) as T
            BackgroundPermissionsViewModel::class.java -> BackgroundPermissionsViewModel(
                permissionsInteractor
            ) as T
            else -> throw IllegalArgumentException("Can't instantiate class $modelClass")
        }
    }
}