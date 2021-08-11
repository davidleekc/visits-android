package com.hypertrack.android.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.hypertrack.android.api.ApiClient
import com.hypertrack.android.models.EMPTY_HISTORY
import com.hypertrack.android.models.History
import com.hypertrack.android.models.HistoryError
import com.hypertrack.android.models.HistoryResult
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.MockData
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.R
import java.time.LocalDate

interface HistoryRepository {
    suspend fun getHistory(date: LocalDate): HistoryResult
    val history: LiveData<Map<LocalDate, History>>
}

class HistoryRepositoryImpl(
    private val apiClient: ApiClient,
    private val crashReportsProvider: CrashReportsProvider,
    private val osUtilsProvider: OsUtilsProvider
) : HistoryRepository {

    override val history = MutableLiveData<Map<LocalDate, History>>(mapOf())

    private val cache = mutableMapOf<LocalDate, History>()

    override suspend fun getHistory(date: LocalDate): HistoryResult {
        if (cache.containsKey(date) && date != LocalDate.now()) {
            return cache.getValue(date)
        } else {
            val res = apiClient.getHistory(
                date,
                osUtilsProvider.getTimeZoneId()
            )
            when (res) {
                is History -> {
                    addToCache(date, res)
                }
                is HistoryError -> {
                    crashReportsProvider.logException(res.error ?: Exception("History error null"))
                }
            }
            return res
        }
    }

    private fun addToCache(date: LocalDate, res: History) {
        cache[date] = res
        Log.v("hypertrack-verbose", "post value ${cache}")
        history.postValue(cache)
    }

}