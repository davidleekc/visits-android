package com.hypertrack.android.ui.screens.visits_management.tabs.places

import android.util.Log
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.base.SingleLiveEvent
import com.hypertrack.android.ui.base.toConsumable
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragmentDirections
import com.hypertrack.android.ui.screens.visits_management.tabs.history.DeviceLocationProvider
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.formatters.DatetimeFormatter
import com.hypertrack.android.utils.formatters.DistanceFormatter
import com.hypertrack.android.utils.formatters.LocalizedDistanceFormatter

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PlacesViewModel(
    private val placesInteractor: PlacesInteractor,
    private val osUtilsProvider: OsUtilsProvider,
    private val locationProvider: DeviceLocationProvider,
    private val distanceFormatter: DistanceFormatter,
    private val datetimeFormatter: DatetimeFormatter,
) : BaseViewModel(osUtilsProvider) {

    private var nextPageToken: String? = null
    private var updateJob: Job? = null

    val placesPage = SingleLiveEvent<Consumable<List<PlaceItem>>?>()

    fun refresh() {
        placesInteractor.invalidateCache()
        init()
    }

    fun init() {
        loadingStateBase.value = false
        loadingStateBase.postValue(false)
        updateJob?.cancel()
        nextPageToken = null
        placesPage.value = null
        placesPage.postValue(null)
        onLoadMore()
    }

    fun createPlacesAdapter(): PlacesAdapter {
        return PlacesAdapter(
            osUtilsProvider,
            locationProvider,
            distanceFormatter,
            datetimeFormatter
        )
    }

    fun onPlaceClick(placeItem: PlaceItem) {
        destination.postValue(
            VisitsManagementFragmentDirections.actionVisitManagementFragmentToPlaceDetailsFragment(
                placeItem.geofence.id
            )
        )
    }

    fun onAddPlaceClicked() {
        destination.postValue(VisitsManagementFragmentDirections.actionVisitManagementFragmentToAddPlaceFragment())
    }

    fun onLoadMore() {
        if ((loadingStateBase.value ?: false) == false) {
            //todo change to viewModelScope (viewModelScope cause bug when launch is not called after geofence creation)
            updateJob = GlobalScope.launch {
                try {
                    if (nextPageToken != null || placesPage.value == null) {
//                        Log.v("hypertrack-verbose", "** loading ${nextPageToken.hashCode()}")
                        loadingStateBase.postValue(true)
                        val res = placesInteractor.loadPage(nextPageToken)
                        nextPageToken = res.paginationToken
//                        Log.v("hypertrack-verbose", "nextPageToken = ${nextPageToken.hashCode()}")
                        placesPage.postValue(Consumable(res.geofences.map { PlaceItem(it) }))
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