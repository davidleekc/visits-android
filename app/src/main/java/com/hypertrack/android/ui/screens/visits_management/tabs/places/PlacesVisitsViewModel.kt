package com.hypertrack.android.ui.screens.visits_management.tabs.places

import androidx.lifecycle.asLiveData
import com.hypertrack.android.api.GeofenceVisit
import com.hypertrack.android.interactors.HistoryInteractor
import com.hypertrack.android.interactors.PlacesVisitsInteractor
import com.hypertrack.android.models.History
import com.hypertrack.android.models.local.LocalGeofenceVisit
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.base.SingleLiveEvent
import com.hypertrack.android.ui.common.util.format
import com.hypertrack.android.ui.common.util.requireValue
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragmentDirections
import com.hypertrack.android.ui.screens.visits_management.tabs.history.TimeDistanceFormatter
import com.hypertrack.android.utils.OsUtilsProvider

import com.hypertrack.android.utils.applyAddAll
import com.hypertrack.android.utils.formatters.DatetimeFormatter
import com.hypertrack.android.utils.formatters.DistanceFormatter
import com.hypertrack.android.utils.formatters.TimeFormatter
import com.hypertrack.android.utils.formatters.prettyFormat
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.Month
import java.time.ZonedDateTime

class PlacesVisitsViewModel(
    private val placesVisitsInteractor: PlacesVisitsInteractor,
    private val historyInteractor: HistoryInteractor,
    private val osUtilsProvider: OsUtilsProvider,
    private val datetimeFormatter: DatetimeFormatter,
    private val distanceFormatter: DistanceFormatter,
    private val timeFormatter: TimeFormatter,
) : BaseViewModel(osUtilsProvider) {

    val adapter = createVisitsAdapter()
    private var adapterData = VisitsData(listOf(), mapOf())

    private var nextPageToken: String? = null
    private var updateJob: Job? = null

    val visitsPage = SingleLiveEvent<Consumable<List<LocalGeofenceVisit>>?>()

    init {
        historyInteractor.errorFlow.asLiveData().observeManaged {
            errorHandler.postConsumable(it)
        }

        historyInteractor.history.observeManaged {
            adapterData = adapterData.newInstanceWithHistory(it)
            adapter.updateData(adapterData)
        }
    }

    fun refresh() {
        placesVisitsInteractor.invalidateCache()
        init()
    }

    fun init() {
        loadingStateBase.value = false
        loadingStateBase.postValue(false)
        updateJob?.cancel()
        nextPageToken = null
        adapter.updateItems(listOf())
        visitsPage.value = null
        visitsPage.postValue(null)
        onLoadMore()
    }

    private fun onVisitClick(visit: LocalGeofenceVisit) {
        destination.postValue(
            VisitsManagementFragmentDirections.actionVisitManagementFragmentToPlaceDetailsFragment(
                visit.geofenceId
            )
        )
    }

    fun onLoadMore() {
        if (loadingStateBase.value == null || loadingStateBase.value == false) {
            //todo change to viewModelScope (viewModelScope cause bug when launch is not called after geofence creation)
            updateJob = GlobalScope.launch {
                try {
                    if (nextPageToken != null || visitsPage.value == null) {
//                        Log.v("hypertrack-verbose", "** loading ${nextPageToken.hashCode()}")
                        loadingStateBase.postValue(true)
                        val res = placesVisitsInteractor.loadPage(nextPageToken)
                        nextPageToken = res.paginationToken
//                        Log.v("hypertrack-verbose", "nextPageToken = ${nextPageToken.hashCode()}")

                        adapterData = adapterData.newInstanceWithNewVisits(res.items)
                        withContext(Dispatchers.Main) {
                            adapter.updateData(adapterData)
                        }

                        visitsPage.postValue(Consumable(res.items))
                        triggerHistoryUpdates()
                        loadingStateBase.postValue(false)
                    }
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        errorHandler.postException(e)
                        loadingStateBase.postValue(false)
                    }
                }
            }
        }
    }

    private fun triggerHistoryUpdates() {
        val history = historyInteractor.history.requireValue()
        for (item in adapter.items.filterIsInstance<Day>()) {
            if (!history.containsKey(item.date)) {
                historyInteractor.loadHistory(item.date)
            }
        }
//        for (item in adapter.items.filterIsInstance<MonthItem>()) {
//            historyInteractor.loadMonthSummary(item.month)
//        }
    }

    private fun createVisitsAdapter(): AllPlacesVisitsAdapter {
        return AllPlacesVisitsAdapter(
            osUtilsProvider,
            datetimeFormatter,
            distanceFormatter,
            timeFormatter
        ) {
            osUtilsProvider.copyToClipboard(it)
        }.apply {
            onItemClickListener = {
                if (it is Visit) {
                    onVisitClick(it.visit)
                }
            }
        }
    }

}

data class VisitsData(
    val visits: List<LocalGeofenceVisit>,
    val dayStats: Map<LocalDate, Int>,
) {
    val adapterData: List<VisitItem>

    val monthStats: Map<Month, Int>

    init {
        val res = mutableListOf<VisitItem>()
        val monthStats = mutableMapOf<Month, Int>()
        val visits = visits.sortedByDescending { it.getDay() }
        if (visits.isNotEmpty()) {
            var currentDay: LocalDate? = null
//            var currentMonth: Month? = null
            val monthPack = mutableListOf<VisitItem>()

            fun submitCurrentMonth() {
                res.addAll(monthPack)

//                if (monthPack.filterIsInstance<Day>().all { dayStats[it.date] != null }) {
//                    monthStats[currentMonth!!] =
//                        monthPack.filterIsInstance<Day>().map { dayStats[it.date]!! }.sum()
//                }

                monthPack.clear()
            }

            for (visit in visits) {
                //new month
//                if (visit.getDay().month != currentMonth) {
//                    //if not first submit previous month
//                    currentMonth?.let {
//                        submitCurrentMonth()
//                    }
//
//                    currentMonth = visit.getDay().month
//                    monthPack.add(MonthItem(currentMonth))
//                }
                //new day
                if (visit.getDay() != currentDay) {
                    currentDay = visit.getDay()
                    monthPack.add(Day(currentDay))
                }
                monthPack.add(Visit(visit))
            }
            submitCurrentMonth()
        }
        adapterData = res
        this.monthStats = monthStats
    }

    fun newInstanceWithNewVisits(newVisits: List<LocalGeofenceVisit>): VisitsData {
        return VisitsData(visits.applyAddAll(newVisits), dayStats)
    }

    fun newInstanceWithHistory(history: Map<LocalDate, History>): VisitsData {
        return VisitsData(visits, history.mapValues { it.value.summary.totalDriveDistance }).also {
//            Log.v(
//                "hypertrack-verbose",
//                it.dayStats.keys.sortedByDescending { it }.map { "${it.dayOfMonth} ${it.month}"  }.toString()
//            )
//            Log.v("hypertrack-verbose", it.monthStats.keys.map { it }.toString())
        }
    }
}

sealed class VisitItem
class Visit(val visit: LocalGeofenceVisit) : VisitItem() {
    override fun toString(): String {
        return "visit ${visit.getDay().prettyFormat()}"
    }
}

class Day(val date: LocalDate) : VisitItem() {
    override fun toString(): String {
        return date.prettyFormat()
    }
}

//class MonthItem(val month: Month) : VisitItem() {
//    override fun toString(): String {
//        return month.toString()
//    }
//}