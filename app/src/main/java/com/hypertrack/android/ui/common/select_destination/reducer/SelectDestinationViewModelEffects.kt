package com.hypertrack.android.ui.common.select_destination.reducer

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.ui.common.HypertrackMapWrapper

// @formatter:off

sealed class Effect
data class DisplayAddress(val address: String) : Effect()
data class Proceed(val placeData: PlaceData) : Effect()
data class MoveMap(val latLng: LatLng, val map: HypertrackMapWrapper) : Effect()
object CloseKeyboard : Effect() {
    override fun toString(): String = javaClass.simpleName
}

object RemoveSearchFocus : Effect() {
    override fun toString(): String = javaClass.simpleName
}

// @formatter:on
