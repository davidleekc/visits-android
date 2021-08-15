package com.hypertrack.android.ui.screens.place_details

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CircleOptions
import com.hypertrack.android.api.GeofenceVisit
import com.hypertrack.android.interactors.GeofenceError
import com.hypertrack.android.interactors.GeofenceSuccess
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.models.Integration
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.base.ZipNotNullableLiveData
import com.hypertrack.android.ui.common.KeyValueItem
import com.hypertrack.android.ui.common.delegates.GeofenceAddressDelegate
import com.hypertrack.android.ui.common.format
import com.hypertrack.android.ui.common.formatDateTime
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.R
import com.squareup.moshi.Moshi
import kotlinx.coroutines.launch

class PlaceDetailsViewModel(
    private val geofenceId: String,
    private val placesInteractor: PlacesInteractor,
    private val osUtilsProvider: OsUtilsProvider,
    private val crashReportsProvider: CrashReportsProvider,
    private val moshi: Moshi
) : BaseViewModel(osUtilsProvider) {

    private val addressDelegate = GeofenceAddressDelegate(osUtilsProvider)

    private val map = MutableLiveData<GoogleMap>()

    val loadingState = MutableLiveData<Boolean>(false)

    private val geofence = MutableLiveData<LocalGeofence>().apply {
        viewModelScope.launch {
            loadingState.postValue(true)
            when (val res = placesInteractor.getGeofence(geofenceId)) {
                is GeofenceSuccess -> {
                    postValue(res.geofence)
                }
                is GeofenceError -> {
                    errorHandler.postException(res.e)
                }
            }
            loadingState.postValue(false)
        }
    }

    val address = Transformations.map(geofence) { geofence ->
        addressDelegate.fullAddress(geofence)
    }

    val metadata: LiveData<List<KeyValueItem>> = Transformations.map(geofence) { geofence ->
        geofence.metadata.toMutableMap().apply {
            put(
                osUtilsProvider.stringFromResource(R.string.place_visits_count),
                geofence.visitsCount.toString()
            )
            put(
                osUtilsProvider.stringFromResource(R.string.created_at),
                geofence.createdAt.formatDateTime()
            )
            put(
                osUtilsProvider.stringFromResource(R.string.coordinates),
                geofence.latLng.format()
            )
        }
            .map { KeyValueItem(it.key, it.value) }.toList()
    }

    val integration: LiveData<Integration?> = Transformations.map(geofence) {
        it.integration
    }

    val visits: LiveData<List<GeofenceVisit>> = Transformations.map(geofence) { geofence ->
        geofence.markers
    }

    val externalMapsIntent = MutableLiveData<Consumable<Intent>>()

    init {
        //todo check leak
        ZipNotNullableLiveData(geofence, map).apply {
            observeForever {
                displayGeofenceLocation(it.first, it.second)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun onMapReady(googleMap: GoogleMap) {
        googleMap.uiSettings.apply {
            isScrollGesturesEnabled = false
            isMyLocationButtonEnabled = true
            isZoomControlsEnabled = true
        }
        try {
            googleMap.isMyLocationEnabled = true
        } catch (_: Exception) {
        }
        map.postValue(googleMap)
    }

    private fun displayGeofenceLocation(geofence: LocalGeofence, googleMap: GoogleMap) {
        geofence.radius?.let { radius ->
            googleMap.addCircle(
                CircleOptions()
                    .center(geofence.latLng)
                    .fillColor(osUtilsProvider.colorFromResource(R.color.colorGeofenceFill))
                    .strokeColor(osUtilsProvider.colorFromResource(R.color.colorGeofence))
                    .strokeWidth(3f)
                    .radius(radius.toDouble())
                    .visible(true)
            )
        }
        googleMap.addCircle(
            CircleOptions()
                .center(geofence.latLng)
                .fillColor(osUtilsProvider.colorFromResource(R.color.colorGeofence))
                .strokeColor(Color.TRANSPARENT)
                .radius(30.0)
                .visible(true)
        )
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(geofence.latLng, 15.0f))
    }

    fun onDirectionsClick() {
        val intent = osUtilsProvider.getMapsIntent(geofence.value!!.latLng)
        intent?.let {
            externalMapsIntent.postValue(Consumable(it))
        }
    }

    fun onAddressClick() {
        if (!address.value.isNullOrEmpty()) {
            osUtilsProvider.copyToClipboard(address.value!!)
        }
    }

    fun onCopyValue(value: String) {
        if (value.isNotEmpty()) {
            osUtilsProvider.copyToClipboard(value)
        }
    }

    fun onCopyVisitIdClick(str: String) {
        osUtilsProvider.copyToClipboard(str)
    }

    fun onIntegrationCopy() {
        integration.value?.let {
            osUtilsProvider.copyToClipboard(it.id)
        }
    }
}