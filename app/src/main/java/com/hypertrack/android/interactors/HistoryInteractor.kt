package com.hypertrack.android.interactors

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.hypertrack.android.api.HistoryResponse
import com.hypertrack.android.models.EMPTY_HISTORY
import com.hypertrack.android.models.History
import com.hypertrack.android.models.HistoryError
import com.hypertrack.android.models.HistoryResult
import com.hypertrack.android.repository.HistoryRepository
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.base.toConsumable
import com.hypertrack.android.ui.common.toHotTransformation
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

interface HistoryInteractor {
    val todayHistory: LiveData<History>
    val history: LiveData<Map<LocalDate, History>>
    val errorFlow: MutableSharedFlow<Consumable<Exception>>
    fun loadTodayHistory()
    fun loadHistory(date: LocalDate)
}

class HistoryInteractorImpl(
    private val historyRepository: HistoryRepository,
) : HistoryInteractor {

    override val todayHistory = MutableLiveData<History>()

    init {
        historyRepository.history.observeForever {
            Log.v("hypertrack-verbose", "observe value ${it}")
        }
    }

    override val history = Transformations.map(historyRepository.history) {
        it.toMutableMap().apply {
            todayHistory.value?.let { put(LocalDate.now(), it) }
        }.toMap()
    }.toHotTransformation().liveData

    override val errorFlow = MutableSharedFlow<Consumable<Exception>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override fun loadTodayHistory() {
        GlobalScope.launch {
            errorFlow.emit(Exception("History error null").toConsumable())
            cancel()
            val res = historyRepository.getHistory(LocalDate.now())
            when (res) {
                is History -> {
                    todayHistory.postValue(res)
                }
                is HistoryError -> {
                    errorFlow.emit((res.error ?: Exception("History error null")).toConsumable())
                }
            }
        }
    }

    override fun loadHistory(date: LocalDate) {
        GlobalScope.launch {
            historyRepository.getHistory(date)
        }
    }
}