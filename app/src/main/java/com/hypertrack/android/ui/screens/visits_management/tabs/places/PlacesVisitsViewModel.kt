package com.hypertrack.android.ui.screens.visits_management.tabs.places

import android.util.Log
import com.hypertrack.android.api.GeofenceMarker
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.interactors.PlacesVisitsInteractor
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.base.SingleLiveEvent
import com.hypertrack.android.ui.base.toConsumable
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragmentDirections
import com.hypertrack.android.ui.screens.visits_management.tabs.history.DeviceLocationProvider
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.TimeDistanceFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PlacesVisitsViewModel(
    private val placesVisitsInteractor: PlacesVisitsInteractor,
    private val osUtilsProvider: OsUtilsProvider,
    private val timeDistanceFormatter: TimeDistanceFormatter,
) : BaseViewModel(osUtilsProvider) {

    private var nextPageToken: String? = null
    private var updateJob: Job? = null

    val visitsPage = SingleLiveEvent<Consumable<List<GeofenceMarker>>?>()

    fun refresh() {
        placesVisitsInteractor.invalidateCache()
        init()
    }

    fun init() {
        loadingStateBase.value = false
        loadingStateBase.postValue(false)
        updateJob?.cancel()
        nextPageToken = null
        visitsPage.value = null
        visitsPage.postValue(null)
        onLoadMore()
    }

    fun createVisitsAdapter(): AllPlacesVisitsAdapter {
        return AllPlacesVisitsAdapter(
            osUtilsProvider,
            timeDistanceFormatter
        ) {
            osUtilsProvider.copyToClipboard(it)
        }
    }

    fun onVisitClick(visit: GeofenceMarker) {
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
                        visitsPage.postValue(Consumable(res.items))
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

}