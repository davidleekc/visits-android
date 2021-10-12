package com.hypertrack.android.interactors

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.hypertrack.android.models.*
import com.hypertrack.android.repository.HistoryRepository
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.base.toConsumable
import com.hypertrack.android.ui.common.util.toHotTransformation
import com.hypertrack.android.utils.OsUtilsProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

interface HistoryInteractor {
    val todayHistory: LiveData<History?>
    val history: LiveData<Map<LocalDate, History>>
    val errorFlow: MutableSharedFlow<Consumable<Exception>>
    fun refreshTodayHistory()
    fun loadHistory(date: LocalDate)
}

//todo test
class HistoryInteractorImpl(
    private val historyRepository: HistoryRepository,
    private val globalScope: CoroutineScope
) : HistoryInteractor {

    //todo update timer
    private var lastUpdate: ZonedDateTime? = null

    override val todayHistory: LiveData<History?> = Transformations.map(historyRepository.history) {
        it[LocalDate.now()]
    }

    override val history = historyRepository.history

    override val errorFlow = MutableSharedFlow<Consumable<Exception>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override fun refreshTodayHistory() {
        //todo test
        if (lastUpdate == null || ChronoUnit.MINUTES.between(
                ZonedDateTime.now(),
                lastUpdate
            ) > UPDATE_TIMEOUT_MINUTES
        ) {
            val updateTime = ZonedDateTime.now()
            globalScope.launch {
                //adds today history to cache
                val res = historyRepository.getHistory(LocalDate.now())
                when (res) {
                    is History -> {
                        lastUpdate = updateTime
                    }
                    is HistoryError -> {
                        errorFlow.emit(
                            (res.error ?: Exception("History error null")).toConsumable()
                        )
                    }
                }
            }
        }
    }

    override fun loadHistory(date: LocalDate) {
        globalScope.launch {
            historyRepository.getHistory(date)
        }
    }

    companion object {
        const val UPDATE_TIMEOUT_MINUTES = 1
    }

}