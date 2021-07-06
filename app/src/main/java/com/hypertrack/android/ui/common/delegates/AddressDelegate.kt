package com.hypertrack.android.ui.common.delegates

import android.location.Address
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.models.local.LocalOrder
import com.hypertrack.android.ui.common.*
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.R

class OrderAddressDelegate(val osUtilsProvider: OsUtilsProvider) {

    //todo nominatim first two parts
    //used as order name in list
    fun shortAddress(order: LocalOrder): String {
        return order.destinationAddress?.nullIfBlank()?.let {
            if (it.length < SHORT_ADDRESS_LIMIT) {
                it
            } else null
        } ?: osUtilsProvider.getPlaceFromCoordinates(
            order.destinationLatLng.latitude,
            order.destinationLatLng.longitude
        )?.toAddressString(short = true, disableCoordinatesFallback = true)
        ?: order.scheduledAt?.let {
            osUtilsProvider.stringFromResource(R.string.order_scheduled_at, it.formatDateTime())
        }
        ?: osUtilsProvider.stringFromResource(R.string.order_address_not_available)
    }

    fun fullAddress(order: LocalOrder): String {
        return order.destinationAddress?.nullIfBlank()
            ?: osUtilsProvider.getPlaceFromCoordinates(
                order.destinationLatLng.latitude,
                order.destinationLatLng.longitude
            )?.toAddressString(disableCoordinatesFallback = true)
            ?: osUtilsProvider.stringFromResource(R.string.address_not_available)
    }

}

class GeofenceAddressDelegate(val osUtilsProvider: OsUtilsProvider) {

    fun shortAddress(geofence: LocalGeofence): String {
        return geofence.address?.nullIfBlank()?.let {
            if (it.length < SHORT_ADDRESS_LIMIT) {
                it
            } else null
        } ?: osUtilsProvider.getPlaceFromCoordinates(
            geofence.latLng.latitude,
            geofence.latLng.longitude
        )?.toAddressString(short = true)
        ?: osUtilsProvider.stringFromResource(R.string.address_not_available)
    }

    fun fullAddress(geofence: LocalGeofence): String {
        return geofence.address?.nullIfBlank()
            ?: osUtilsProvider.getPlaceFromCoordinates(
                geofence.latLng.latitude,
                geofence.latLng.longitude
            )?.toAddressString()
            ?: osUtilsProvider.stringFromResource(R.string.address_not_available)
    }

}

fun String?.parseNominatimAddress(): String? {
    return this?.let {
        split(",").map { it.trim() }.take(2).joinToString(", ")
    }
}

fun Address.toAddressString(
    strictMode: Boolean = false,
    short: Boolean = false,
    disableCoordinatesFallback: Boolean = false,
): String? {
    if (strictMode && thoroughfare == null) {
        return null
    }

    val firstPart = if (!short) {
        locality.wrapIfPresent(end = ", ")
    } else {
        ""
    }

    val secondPart = if (!short) {
        //long
        if (thoroughfare != null) {
            "$thoroughfare${subThoroughfare.wrapIfPresent(start = ", ")}"
        } else {
            if (!disableCoordinatesFallback) {
                LatLng(latitude, longitude).format()
            } else {
                ""
            }
        }
    } else {
        //short
        if (thoroughfare != null) {
            "$thoroughfare${subThoroughfare.wrapIfPresent(start = ", ")}"
        } else {
            if (!disableCoordinatesFallback) {
                LatLng(latitude, longitude).format()
            } else {
                locality.wrapIfPresent()
            }
        }
    }

    return "$firstPart$secondPart".nullIfBlank()
}

fun Place.toAddressString(): String {
    val locality =
        addressComponents?.asList()?.filter { "locality" in it.types }?.firstOrNull()?.name
            ?: addressComponents?.asList()?.filter { "administrative_area_level_1" in it.types }
                ?.firstOrNull()?.name
            ?: addressComponents?.asList()?.filter { "administrative_area_level_2" in it.types }
                ?.firstOrNull()?.name
            ?: addressComponents?.asList()?.filter { "political" in it.types }?.firstOrNull()?.name
    val thoroughfare =
        addressComponents?.asList()?.filter { "route" in it.types }?.firstOrNull()?.name
    val subThoroughfare =
        addressComponents?.asList()?.filter { "street_number" in it.types }?.firstOrNull()?.name

    val localityString = (locality?.let { "$it, " } ?: "")
    val address = if (thoroughfare == null) {
        latLng?.format() ?: ""
    } else {
        " $thoroughfare${subThoroughfare?.let { ", $it" } ?: ""}"
    }
    return "$localityString$address"
}

fun String?.wrapIfPresent(start: String? = null, end: String? = null): String {
    return this?.let { "${start.wrapIfPresent()}$it${end.wrapIfPresent()}" } ?: ""
}

const val SHORT_ADDRESS_LIMIT = 50