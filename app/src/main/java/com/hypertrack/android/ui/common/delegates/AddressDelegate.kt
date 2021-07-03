package com.hypertrack.android.ui.common.delegates

import com.hypertrack.android.models.local.LocalOrder
import com.hypertrack.android.ui.common.toShortAddressString
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.R

class OrderAddressDelegate(val osUtilsProvider: OsUtilsProvider) {

    fun shortAddress(order: LocalOrder): String {
        return order.destinationAddress?.let {
            if (it.length < SHORT_ADDRESS_LIMIT) {
                it
            } else null
        } ?: osUtilsProvider.getPlaceFromCoordinates(
            order.destinationLatLng.latitude,
            order.destinationLatLng.longitude
        )?.toShortAddressString()
        ?: osUtilsProvider.stringFromResource(R.string.address_not_available)
    }

    fun fullAddress(order: LocalOrder): String {
        return order.destinationAddress
            ?: osUtilsProvider.getPlaceFromCoordinates(
                order.destinationLatLng.latitude,
                order.destinationLatLng.longitude
            )?.toShortAddressString()
            ?: osUtilsProvider.stringFromResource(R.string.address_not_available)
    }

}

const val SHORT_ADDRESS_LIMIT = 50