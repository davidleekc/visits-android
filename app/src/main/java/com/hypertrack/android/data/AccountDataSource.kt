package com.hypertrack.android.data

import com.hypertrack.android.repository.*

interface AccountDataStorage {

    fun getAccountData(): AccountData
    fun saveAccountData(accountData: AccountData)

    fun loadUser(): User?
    fun saveUser(user: User)

    fun getDriverValue(): Driver
    fun saveDriver(driverModel: Driver)

    fun persistRepository(repo: AccessTokenRepository)
    fun restoreRepository(): BasicAuthAccessTokenRepository?
}