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
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.base.postValue
import com.hypertrack.android.ui.common.Tab
import com.hypertrack.android.ui.common.delegates.GooglePlaceAddressDelegate
import com.hypertrack.android.ui.common.select_destination.DestinationData
import com.hypertrack.android.ui.screens.add_order.AddOrderFragmentDirections
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.TripCreationScope
import com.hypertrack.logistics.android.github.NavGraphDirections
import kotlinx.coroutines.launch


class AddOrderInfoViewModel(
    private val params: Params,
    baseDependencies: BaseViewModelDependencies,
    private val tripsInteractor: TripsInteractor,
) : BaseViewModel(baseDependencies) {

    private val addressDelegate = GooglePlaceAddressDelegate(osUtilsProvider)

    val destinationData = params.destinationData

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
                postValue(addressDelegate.strictAddress(it))
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
                    loadingState.postValue(true)
                    val res = tripsInteractor.addOrderToTrip(
                        tripId = params.tripId!!,
                        destinationData.latLng,
                        address
                    )
                    when (res) {
                        is AddOrderSuccess -> {
                            destination.postValue(
                                NavGraphDirections.actionGlobalVisitManagementFragment(Tab.CURRENT_TRIP)
                            )
                        }
                        is AddOrderError -> {
                            errorHandler.postException(res.e)
                        }
                    }
                    loadingState.postValue(false)
                }
            } else {
                MyApplication.injector.tripCreationScope = TripCreationScope(destinationData)
                destination.postValue(
                    AddOrderFragmentDirections
                        .actionGlobalVisitManagementFragment(Tab.CURRENT_TRIP)
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