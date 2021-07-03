package com.hypertrack.android.ui.common

import android.location.Address
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.hypertrack.android.models.Location
import kotlin.math.pow
import kotlin.math.round

object LocationUtils {
    fun distanceMeters(location: Location?, location1: Location?): Int? {
        try {
            if (location != null && location1 != null
                && !(location.latitude == 0.0 && location.longitude == 0.0)
                && !(location1.latitude == 0.0 && location1.longitude == 0.0)
            ) {
                val res = FloatArray(3)
                android.location.Location.distanceBetween(
                    location.latitude,
                    location.longitude,
                    location1.latitude,
                    location1.longitude,
                    res
                );
                return res[0].toInt()
            } else {
                return null
            }
        } catch (_: Exception) {
            return null
        }
    }

    fun distanceMeters(latLng: LatLng?, latLng1: LatLng?): Int? {
        try {
            if (latLng != null && latLng1 != null
                && !(latLng.latitude == 0.0 && latLng.longitude == 0.0)
                && !(latLng1.latitude == 0.0 && latLng1.longitude == 0.0)
            ) {
                val res = FloatArray(3)
                android.location.Location.distanceBetween(
                    latLng.latitude,
                    latLng.longitude,
                    latLng1.latitude,
                    latLng1.longitude,
                    res
                );
                return res[0].toInt()
            } else {
                return null
            }
        } catch (_: Exception) {
            return null
        }
    }
}

fun android.location.Location.toLatLng(): LatLng {
    return LatLng(latitude, longitude)
}

fun Address.toAddressString(): String {
    val localityString = (locality?.let { "$it, " } ?: "")
    val address = if (thoroughfare == null) {
        " ${LatLng(latitude, longitude).format()}"
    } else {
        " $thoroughfare${subThoroughfare?.let { ", $it" } ?: ""}"
    }
    return "$localityString$address"
}

fun Address.toShortAddressString(): String {
    val address = if (thoroughfare == null) {
        " ${LatLng(latitude, longitude).format()}"
    } else {
        "$thoroughfare${subThoroughfare?.let { ", $it" } ?: ""}"
    }
    return address
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

fun Double.roundToSign(n: Int): Double {
    return round(this * 10.0.pow(n)) / (10.0.pow(n))
}

fun LatLng.format(): String {
    return "${latitude.roundToSign(5)}, ${longitude.roundToSign(5)}"
}

