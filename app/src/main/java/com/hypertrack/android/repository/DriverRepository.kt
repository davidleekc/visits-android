package com.hypertrack.android.repository

import com.hypertrack.android.data.AccountDataStorage
import com.hypertrack.android.ui.common.nullIfBlank
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.ServiceLocator
import com.squareup.moshi.JsonClass

class DriverRepository(
    private val accountRepository: AccountRepository,
    private val serviceLocator: ServiceLocator,
    private val accountDataStorage: AccountDataStorage,
    private val crashReportsProvider: CrashReportsProvider,
) {
    private var _user: User? = null
    val user: User?
        get() = _user

    init {
        _user = accountDataStorage.loadUser() ?: accountDataStorage.getDriverValue().let {
            if (it.driverId.isBlank()) {
                null
            } else {
                it
            }
        }?.toUser()
    }

    fun setUserData(
        metadata: Map<String, Any>? = null,
        email: String? = null,
        phoneNumber: String? = null
    ) {
        check(email != null || phoneNumber != null) {
            "User data must have email or phone number"
        }
        _user = User(
            email = email,
            phoneNumber = phoneNumber,
            metadata = metadata
        ).also {
            accountDataStorage.saveUser(it)
        }
        serviceLocator.getHyperTrackService(accountRepository.publishableKey).apply {
            val name = (email?.split("@")?.first() ?: phoneNumber)
            setDeviceInfo(
                name = name,
                email = email,
                phoneNumber = phoneNumber,
                metadata = metadata
            )
        }
    }

    val hasUserData: Boolean
        get() = _user != null

    val username: String
        get() = _user!!.let {
            it.email ?: it.phoneNumber ?: it.driverId ?: ""
        }

    companion object {
        const val TAG = "DriverRepo"
    }
}

@JsonClass(generateAdapter = true)
data class User(
    val email: String? = null,
    val phoneNumber: String? = null,
    val metadata: Map<String, Any>? = null,
    //legacy
    val driverId: String? = null,
)

//todo remove (v0.9.20)
@JsonClass(generateAdapter = true)
data class Driver(
    val driverId: String
) {
    fun toUser(): User {
        return User(
            driverId = driverId,
            email = if (driverId.contains("@")) driverId else null
        )
    }
}
