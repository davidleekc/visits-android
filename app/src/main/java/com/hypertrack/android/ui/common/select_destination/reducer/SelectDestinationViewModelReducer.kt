package com.hypertrack.android.ui.common.select_destination.reducer

import android.graphics.Insets.add
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.ui.common.HypertrackMapWrapper
import com.hypertrack.android.utils.IllegalActionException

class SelectDestinationViewModelReducer {

    fun reduceAction(state: State, action: Action): ReducerResult {
        return when (action) {
            is UserLocationReceived -> {
                reduce(action, state)
            }
            is MapReadyAction -> {
                when (state) {
                    is MapNotReady -> {
                        val userLocationEffects = getMapMoveEffectsIfNeeded(
                            true,
                            state.userLocation,
                            action.map
                        )

                        MapReady(
                            action.map,
                            state.userLocation,
                            LocationSelected(
                                action.cameraPosition,
                                action.address
                            ),
                            MapFlow,
                            waitingForUserLocationMove = userLocationEffects.isEmpty()
                        ).withEffects(userLocationEffects + setOf(HideProgressbar))
                    }
                    is MapReady -> throw IllegalActionException(action, state)
                }
            }
            is MapCameraMoved -> {
                when (state) {
                    is MapReady -> {
                        when (state.flow) {
                            is AutocompleteFlow -> state.asReducerResult()
                            MapFlow -> MapReady(
                                state.map,
                                state.userLocation,
                                LocationSelected(
                                    action.latLng,
                                    action.address
                                ),
                                MapFlow,
                                //if user performed map move or clicked a place (which leads to programmatic move)
                                //we don't need to move map to his location anymore
                                //unless it was first map movement near zero coordinates on map init
                                waitingForUserLocationMove = !action.isNearZero
                            ).withEffects(
                                if (!action.isProgrammatic) {
                                    setOf(DisplayLocationInfo(action.address, null))
                                } else {
                                    //if map moved programmatically then state should have display info effect sent
                                    setOf()
                                }
                            )
                        }
                    }
                    is MapNotReady -> throw IllegalActionException(action, state)
                }
            }
            is PlaceSelectedAction -> {
                when (state) {
                    is MapNotReady -> throw IllegalActionException(action, state)
                    is MapReady -> {
                        val place = PlaceSelected(
                            latLng = action.latLng,
                            displayAddress = action.displayAddress,
                            strictAddress = action.strictAddress,
                            name = action.name,
                        )

                        state.withPlaceSelected(place, MapFlow)
                            .withEffects(
                                CloseKeyboard,
                                ClearSearchQuery,
                                RemoveSearchFocus,
                                MoveMap(action.latLng, state.map),
                                DisplayLocationInfo(
                                    address = place.displayAddress,
                                    placeName = place.name
                                ),
                            )
                    }
                }
            }
            is MapClicked -> {
                when (state) {
                    is MapNotReady -> throw IllegalActionException(action, state)
                    is MapReady -> when (state.flow) {
                        MapFlow -> state.asReducerResult()
                        is AutocompleteFlow -> state.withMapFlow(MapFlow)
                            .withEffects(
                                CloseKeyboard,
                                ClearSearchQuery,
                                RemoveSearchFocus,
                                DisplayLocationInfo(action.address, null)
                            )
                    }
                }
            }
            is SearchQueryChanged -> {
                when (state) {
                    is MapNotReady -> throw IllegalActionException(action, state)
                    is MapReady -> state.withAutocompleteFlow(AutocompleteFlow(action.results))
                        .asReducerResult()
                }
            }
            is AutocompleteError -> {
                when (state) {
                    is MapNotReady -> throw IllegalActionException(action, state)
                    is MapReady -> state.withMapFlow(MapFlow)
                        .withEffects(CloseKeyboard)
                }
            }
            ConfirmClicked -> {
                when (state) {
                    is MapNotReady -> throw IllegalActionException(action, state)
                    is MapReady -> state.withEffects(
                        CloseKeyboard,
                        Proceed(state.placeData)
                    )
                }
            }
        }
    }

    private fun reduce(action: UserLocationReceived, state: State): ReducerResult {
        val userLocation = UserLocation(action.latLng, action.address)
        return when (state) {
            is MapNotReady -> state.withUserLocation(userLocation).asReducerResult()
            is MapReady -> state.withUserLocation(userLocation).withEffects(
                getMapMoveEffectsIfNeeded(
                    state.waitingForUserLocationMove,
                    userLocation,
                    state.map
                )
            )
        }
    }

    private fun getMapMoveEffectsIfNeeded(
        waitingForUserLocationMove: Boolean,
        userLocation: UserLocation?,
        map: HypertrackMapWrapper
    ): Set<Effect> {
        return if (waitingForUserLocationMove && userLocation != null) {
            setOf(
                createMapMoveEffect(map, userLocation.latLng),
                DisplayLocationInfo(userLocation.address, null)
            )
        } else {
            setOf()
        }
    }

    private fun createMapMoveEffect(
        map: HypertrackMapWrapper,
        userLocation: LatLng,
    ): MoveMap {
        return MoveMap(userLocation, map)
    }

    companion object {
        val INITIAL_STATE = MapNotReady(null)
    }
}

class ReducerResult(val newState: State, val effects: Set<Effect>) {
    constructor(newState: State) : this(newState, setOf())
}

fun State.asReducerResult(): ReducerResult {
    return ReducerResult(this)
}

fun State.withEffects(effects: Set<Effect>): ReducerResult {
    return ReducerResult(this, effects)
}

fun State.withEffects(vararg effect: Effect): ReducerResult {
    return ReducerResult(this, effect.toMutableSet())
}


