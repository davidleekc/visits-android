package com.hypertrack.android.ui.screens.add_place_info

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.models.Integration
import com.hypertrack.android.models.local.LocalGeofenceJson
import com.hypertrack.android.repository.CreateGeofenceError
import com.hypertrack.android.repository.CreateGeofenceSuccess
import com.hypertrack.android.repository.IntegrationsRepository
import com.hypertrack.android.repository.PlacesRepository
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.SingleLiveEvent
import com.hypertrack.android.ui.common.Tab
import com.hypertrack.android.ui.common.toAddressString
import com.hypertrack.android.ui.screens.add_place.AddPlaceFragmentDirections
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragment
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.ResultEmptySuccess
import com.hypertrack.android.utils.ResultError
import com.hypertrack.android.utils.ResultSuccess
import com.hypertrack.logistics.android.github.R
import com.squareup.moshi.Moshi
import kotlinx.coroutines.launch


class AddPlaceInfoViewModel(
    private val latLng: LatLng,
    private val initialAddress: String?,
    private val _name: String?,
    private val placesInteractor: PlacesInteractor,
    private val integrationsRepository: IntegrationsRepository,
    private val osUtilsProvider: OsUtilsProvider,
    private val moshi: Moshi,
) : BaseViewModel(osUtilsProvider) {

    private var hasIntegrations = MutableLiveData<Boolean?>(false)

    val loadingState = MutableLiveData<Boolean>(true)

    //todo persist state in create geofence scope
    val address = MutableLiveData<String?>().apply {
        if (initialAddress != null) {
            postValue(initialAddress)
        } else {
            osUtilsProvider.getPlaceFromCoordinates(latLng.latitude, latLng.longitude)?.let {
                postValue(it.toAddressString())
            }
        }
    }
    val name = MutableLiveData<String>().apply {
        _name?.let {
            postValue(_name)
        }
    }
    val radius = MutableLiveData<Int>(PlacesInteractor.DEFAULT_RADIUS_METERS)
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
        Validation({ errorHandler.postText(R.string.add_place_geofence_radius_validation_error) }) {
            radius.value?.let { radius -> radius > 50 && radius < 100000 } ?: false
        },
        Validation({ errorHandler.postText(R.string.place_info_confirm_disabled) }) {
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

    fun onConfirmClicked(name: String, address: String, description: String) {
        validations.ifValid {
            viewModelScope.launch {
                loadingState.postValue(true)

                val res = placesInteractor.createGeofence(
                    latitude = latLng.latitude,
                    longitude = latLng.longitude,
                    radius = radius.value,
                    name = name,
                    address = address,
                    description = description,
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
                        errorHandler.postException(res.e)
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
            radius.postValue(text.toInt())
        } catch (e: Exception) {
            errorHandler.postException(e)
        }
    }

    private fun shouldShowGeofenceName(): Boolean {
        return if (hasIntegrations.value == true) {
            integration.value == null
        } else {
            true
        }
    }
}

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

