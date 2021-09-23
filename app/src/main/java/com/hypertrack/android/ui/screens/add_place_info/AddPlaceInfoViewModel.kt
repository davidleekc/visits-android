package com.hypertrack.android.ui.screens.add_place_info

import android.annotation.SuppressLint
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.models.Integration
import com.hypertrack.android.repository.CreateGeofenceError
import com.hypertrack.android.repository.CreateGeofenceSuccess
import com.hypertrack.android.repository.IntegrationsRepository
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.common.Tab
import com.hypertrack.android.ui.common.delegates.GooglePlaceAddressDelegate
import com.hypertrack.android.ui.screens.add_place.AddPlaceFragmentDirections
import com.hypertrack.android.utils.*
import com.hypertrack.logistics.android.github.R
import com.squareup.moshi.Moshi
import kotlinx.coroutines.launch


class AddPlaceInfoViewModel(
    private val latLng: LatLng,
    private val initialAddress: String?,
    private val _name: String?,
    private val placesInteractor: PlacesInteractor,
    private val integrationsRepository: IntegrationsRepository,
    private val distanceFormatter: DistanceFormatter,
    private val osUtilsProvider: OsUtilsProvider,
    private val moshi: Moshi,
) : BaseViewModel(osUtilsProvider) {

    private val addressDelegate = GooglePlaceAddressDelegate(osUtilsProvider)

    val hasIntegrations = MutableLiveData<Boolean?>(false)
    val loadingState = MutableLiveData<Boolean>(true)
    val adjacentGeofenceDialog = MutableLiveData<Consumable<GeofenceCreationParams>>()

    //todo persist state in create geofence scope
    val address = MutableLiveData<String?>().apply {
        if (initialAddress != null) {
            postValue(initialAddress)
        } else {
            osUtilsProvider.getPlaceFromCoordinates(latLng.latitude, latLng.longitude)?.let {
                postValue(addressDelegate.strictAddress(it))
            }
        }
    }
    val name = Transformations.map(hasIntegrations) {
        if (it == false && _name != null) {
            _name
        } else {
            null
        }
    }

    val radius = MutableLiveData<Int?>(PlacesInteractor.DEFAULT_RADIUS)
    val integration = MutableLiveData<Integration?>(null)

    val enableConfirmButton = MediatorLiveData<Boolean>().apply {
        addSource(hasIntegrations) {
            postValue(validations.all { it.checkIfValid() })
        }
        addSource(integration) {
            postValue(validations.all { it.checkIfValid() })
        }
        addSource(address) {
            postValue(validations.all { it.checkIfValid() })
        }
        addSource(radius) {
            postValue(validations.all { it.checkIfValid() })
        }
    }

    val showGeofenceNameField = MediatorLiveData<Boolean>().apply {
        addSource(hasIntegrations) {
            postValue(shouldShowGeofenceName())
        }
        addSource(integration) {
            postValue(shouldShowGeofenceName())
        }
    }

    private val validations = listOf(
        Validation({
            errorHandler.postText(
                osUtilsProvider.stringFromResource(
                    R.string.add_place_geofence_radius_validation_error,
                    distanceFormatter.formatDistance(PlacesInteractor.MIN_RADIUS),
                    distanceFormatter.formatDistance(PlacesInteractor.MAX_RADIUS)
                )
            )
        }) {
            radius.value?.let { radius ->
                radius > PlacesInteractor.MIN_RADIUS && radius < PlacesInteractor.MAX_RADIUS
            } ?: false
        },
        Validation({ errorHandler.postText(R.string.add_place_info_confirm_disabled) }) {
            if (hasIntegrations.value == true) {
                integration.value != null
            } else {
                hasIntegrations.value != null
            }
        }
    )

    init {
        viewModelScope.launch {
            loadingState.postValue(true)
            val res = integrationsRepository.hasIntegrations()
            when (res) {
                is ResultSuccess -> {
                    hasIntegrations.postValue(res.value)
                    loadingState.postValue(false)
                }
                is ResultError -> {
                    errorHandler.postException(res.exception)
                    loadingState.postValue(false)
                    hasIntegrations.postValue(null)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun onMapReady(googleMap: GoogleMap) {
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13f))
        googleMap.addMarker(MarkerOptions().position(latLng))
    }

    fun onConfirmClicked(params: GeofenceCreationParams) {
        validations.ifValid {
            viewModelScope.launch {
                loadingState.postValue(true)

                //todo test
                if (placesInteractor.adjacentGeofencesAllowed) {
                    placesInteractor.hasAdjacentGeofence(latLng, radius.value!!).let { has ->
                        if (!has) {
                            proceedCreatingGeofence(params)
                        } else {
                            loadingState.postValue(false)
                            adjacentGeofenceDialog.postValue(Consumable(params))
                        }
                    }
                } else {
                    placesInteractor.blockingHasAdjacentGeofence(latLng, radius.value!!)
                        .let { has ->
                            if (!has) {
                                proceedCreatingGeofence(params)
                            } else {
                                loadingState.postValue(false)
                                errorHandler.postText(R.string.add_place_info_adjacent_geofence_error)
                            }
                        }
                }


            }
        }
    }

    fun onAddIntegration(): Boolean {
        return if (hasIntegrations.value == true) {
            destination.postValue(
                AddPlaceInfoFragmentDirections.actionAddPlaceInfoFragmentToAddIntegrationFragment()
            )
            true
        } else {
            false
        }
    }

    fun onIntegrationAdded(integration: Integration) {
        this.integration.postValue(integration)
    }

    fun onDeleteIntegrationClicked() {
        integration.postValue(null)
    }

    fun onAddressChanged(address: String) {
        this.address.postValue(address)
    }

    fun onRadiusChanged(text: String) {
        try {
            if (text.isNotBlank()) {
                radius.postValue(text.toInt())
            } else {
                radius.postValue(null)
            }
        } catch (e: Exception) {
            radius.postValue(null)
            errorHandler.postException(e)
        }
    }

    private suspend fun proceedCreatingGeofence(params: GeofenceCreationParams) {
        val res = placesInteractor.createGeofence(
            latitude = latLng.latitude,
            longitude = latLng.longitude,
            radius = radius.value,
            name = params.name,
            address = params.address,
            description = params.description,
            integration = integration.value
        )
        loadingState.postValue(false)
        when (res) {
            is CreateGeofenceSuccess -> {
                destination.postValue(
                    AddPlaceFragmentDirections.actionGlobalVisitManagementFragment(
                        Tab.PLACES
                    )
                )
            }
            is CreateGeofenceError -> {
                loadingState.postValue(false)
                errorHandler.postException(res.e)
            }
        }
    }

    private fun shouldShowGeofenceName(): Boolean {
        return if (hasIntegrations.value == true) {
            integration.value == null
        } else {
            true
        }
    }

    fun onGeofenceDialogYes(params: GeofenceCreationParams) {
        viewModelScope.launch {
            proceedCreatingGeofence(params)
        }
    }

}

class GeofenceCreationParams(
    val name: String,
    val address: String,
    val description: String
)

class Validation(val error: () -> Unit, val check: () -> Boolean) {
    fun checkIfValid(): Boolean {
        return check.invoke()
    }
}

fun List<Validation>.ifValid(action: () -> Unit) {
    forEach {
        if (!it.checkIfValid()) {
            it.error.invoke()
            return
        }
    }
    action.invoke()
}

