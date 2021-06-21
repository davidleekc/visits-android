package com.hypertrack.android.ui.screens.add_place

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.location.Location
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.net.PlacesClient
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.interactors.PlacesInteractorImpl
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.common.mixins.ClusterManagerMixin
import com.hypertrack.android.ui.common.mixins.GeofenceClusterItem
import com.hypertrack.android.ui.common.nullIfEmpty
import com.hypertrack.android.ui.screens.select_destination.DestinationData
import com.hypertrack.android.ui.screens.select_destination.SelectDestinationViewModel
import com.hypertrack.android.ui.screens.visits_management.tabs.history.DeviceLocationProvider
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.BuildConfig
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import net.sharewire.googlemapsclustering.ClusterItem
import net.sharewire.googlemapsclustering.ClusterManager


class AddPlaceViewModel(
    private val osUtilsProvider: OsUtilsProvider,
    private val placesClient: PlacesClient,
    private val deviceLocationProvider: DeviceLocationProvider,
    private val placesInteractor: PlacesInteractor
) : SelectDestinationViewModel(
    placesInteractor,
    osUtilsProvider,
    placesClient,
    deviceLocationProvider
), ClusterManagerMixin<GeofenceClusterItem> {

    override val loadingStateBase = placesInteractor.isLoadingForLocation

    override val errorBase = MediatorLiveData<Consumable<String>>().apply {
        addSource(placesInteractor.errorFlow.asLiveData()) { e ->
            map.value?.let { onCameraMoved(it) }
            postValue(e.map {
                osUtilsProvider.getErrorMessage(it)
            })
        }
    }

    override fun proceed(destinationData: DestinationData) {
        destination.postValue(
            AddPlaceFragmentDirections.actionAddPlaceFragmentToAddPlaceInfoFragment(
                destinationData
            )
        )
    }

}