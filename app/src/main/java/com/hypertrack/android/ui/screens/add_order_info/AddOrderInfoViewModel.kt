package com.hypertrack.android.ui.screens.add_order_info

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.MarkerOptions
import com.hypertrack.android.interactors.AddOrderError
import com.hypertrack.android.interactors.AddOrderSuccess
import com.hypertrack.android.interactors.TripsInteractor
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.SingleLiveEvent
import com.hypertrack.android.ui.common.Tab
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.ui.common.toAddressString
import com.hypertrack.android.ui.common.toNullableAddressString
import com.hypertrack.android.ui.screens.add_order.AddOrderFragmentDirections
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.TripCreationScope
import com.hypertrack.logistics.android.github.NavGraphDirections
import kotlinx.coroutines.launch


class AddOrderInfoViewModel(
    private val params: Params,
    private val tripsInteractor: TripsInteractor,
    private val osUtilsProvider: OsUtilsProvider,
) : BaseViewModel() {

    val destinationData = params.destinationData

    //todo to baseVM
    val error = SingleLiveEvent<String>()

    //todo persist state in create order scope
    val address = MutableLiveData<String?>().apply {
        if (destinationData.address != null) {
            postValue(destinationData.address)
        } else {
            osUtilsProvider.getPlaceFromCoordinates(
                destinationData.latLng.latitude,
                destinationData.latLng.longitude
            )?.let {
                //todo set edittext hint with partial address
                postValue(it.toNullableAddressString())
            }
        }
    }
    val enableConfirmButton = MutableLiveData<Boolean>(shouldEnableConfirmButton())

    @SuppressLint("MissingPermission")
    fun onMapReady(googleMap: GoogleMap) {
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destinationData.latLng, 13f))
        googleMap.addMarker(MarkerOptions().position(destinationData.latLng))
    }

    fun onConfirmClicked(address: String) {
        if (enableConfirmButton.value!!) {
            if (!params.isNewTrip) {
                viewModelScope.launch {
                    loadingStateBase.postValue(true)
                    val res = tripsInteractor.addOrderToTrip(
                        tripId = params.tripId!!,
                        destinationData.latLng,
                        address
                    )
                    when (res) {
                        is AddOrderSuccess -> {
                            destination.postValue(
                                NavGraphDirections.actionGlobalVisitManagementFragment(Tab.MAP)
                            )
                        }
                        is AddOrderError -> {
                            error.postValue(osUtilsProvider.getErrorMessage(res.e))
                        }
                    }
                    loadingStateBase.postValue(false)
                }
            } else {
                MyApplication.injector.tripCreationScope = TripCreationScope(destinationData)
                destination.postValue(
                    AddOrderFragmentDirections
                        .actionGlobalVisitManagementFragment(Tab.MAP)
                )
            }
        } else {
//            error.postValue(osUtilsProvider.getString(R.string.place_info_confirm_disabled))
        }
    }

    fun onAddressChanged(address: String) {
        if (this.address.value != address) {
            this.address.postValue(address)
        }
    }

    private fun shouldEnableConfirmButton(): Boolean {
        return true
    }

    class Params(
        val destinationData: DestinationData,
        val tripId: String?
    ) {
        val isNewTrip = tripId == null
    }

}