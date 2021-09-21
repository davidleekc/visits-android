package com.hypertrack.android.ui.common.select_destination.reducer

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.ui.common.HypertrackMapWrapper
import com.hypertrack.android.ui.common.select_destination.GooglePlaceModel
import com.hypertrack.android.utils.AlgBoolean
import com.hypertrack.android.utils.NonEmptyList

// @formatter:off

sealed class State
data class MapLocationState(
    val map: MapState,
    val userLocation: LatLng?,
    val initialMovePerformed: AlgBoolean
)

sealed class MapState
data class MapReady(
    val mapWrapper: HypertrackMapWrapper,
    val cameraPosition: LatLng,
    val cameraPositionAddress: String
) : MapState()

object MapNotReady : MapState() {
    override fun toString(): String = javaClass.simpleName
}

data class Initial(
    val mapLocationState: MapLocationState,
    val query: String?
) : State()

data class MapIsActive(
    val mapLocationState: MapLocationState,
    val placeData: PlaceData
) : State()

sealed class PlaceData
data class PlaceSelected(
    val displayAddress: String,
    val strictAddress: String?,
    val name: String?,
    val latLng: LatLng
) : PlaceData()

data class LocationSelected(
    val latLng: LatLng,
    val address: String,
) : PlaceData()

data class LocationSelectedWithUnsuccesfulQuery(
    val mapState: MapReady,
    val query: String,
) : PlaceData()

data class AutocompleteIsActive(
    val mapLocationState: MapLocationState,
    val query: String,
    val places: NonEmptyList<GooglePlaceModel>
) : State()

data class Confirmed(val placeData: PlaceData, val lastState: State) : State()

val State.userLocation: LatLng?
    get() {
        return when (this) {
            is AutocompleteIsActive -> this.mapLocationState.userLocation
            is Initial -> this.mapLocationState.userLocation
            is MapIsActive -> this.mapLocationState.userLocation
            is Confirmed -> null
        }
    }

// @formatter:on

fun Initial.withUserLocation(userLocation: LatLng): Initial {
    return copy(mapLocationState = mapLocationState.copy(userLocation = userLocation))
}

fun MapIsActive.withUserLocation(userLocation: LatLng): MapIsActive {
    return copy(mapLocationState = mapLocationState.copy(userLocation = userLocation))
}

fun AutocompleteIsActive.withUserLocation(userLocation: LatLng): AutocompleteIsActive {
    return copy(mapLocationState = mapLocationState.copy(userLocation = userLocation))
}

fun Initial.withMap(mapReady: MapReady): Initial {
    return copy(mapLocationState = mapLocationState.copy(map = mapReady))
}

fun MapIsActive.withMap(mapReady: MapReady): MapIsActive {
    return copy(mapLocationState = mapLocationState.copy(map = mapReady))
}

fun AutocompleteIsActive.withMap(mapReady: MapReady): AutocompleteIsActive {
    return copy(mapLocationState = mapLocationState.copy(map = mapReady))
}