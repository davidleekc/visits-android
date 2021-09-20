package com.hypertrack.android.ui.common.select_destination.reducer

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.ui.common.util.isNearZero
import com.hypertrack.android.utils.False
import com.hypertrack.android.utils.IllegalActionException

class SelectDestinationViewModelReducer {

    fun reduceAction(state: State, action: Action): ReducerResult {
        return when (action) {
            is UserLocation -> {
                reduce(action, state)
            }
            is MapReadyAction -> {
                val mapReady = MapReady(
                    action.map,
                    action.cameraPosition,
                    action.address
                )

                when (state) {
                    is Initial -> state.withMap(mapReady).withEffects(
                        getMapMoveEffectIfNeeded(
                            state.mapLocationState,
                            mapReady,
                            state.mapLocationState.userLocation
                        )
                    )
                    is MapIsActive -> state.withMap(mapReady).withEffects(
                        getMapMoveEffectIfNeeded(
                            state.mapLocationState,
                            mapReady,
                            state.mapLocationState.userLocation
                        )
                    )
                    is AutocompleteIsActive -> state.withMap(mapReady).withEffects(
                        getMapMoveEffectIfNeeded(
                            state.mapLocationState,
                            mapReady,
                            state.mapLocationState.userLocation
                        )
                    )
                    is Confirmed -> state.asReducerResult()
                }
            }
            is MapCameraMoved -> {
                if (!action.latLng.isNearZero()) {
                    when (state) {
                        is Initial -> MapIsActive(
                            reduce(
                                action,
                                state.mapLocationState
                            ),
                            LocationSelected(action.latLng, action.address)
                        )
                        is AutocompleteIsActive -> MapIsActive(
                            reduce(
                                action,
                                state.mapLocationState
                            ),
                            LocationSelected(action.latLng, action.address)
                        )
                        is MapIsActive -> MapIsActive(
                            reduce(
                                action,
                                state.mapLocationState
                            ),
                            LocationSelected(action.latLng, action.address)
                        )
                        is Confirmed -> state
                    }.withEffects(DisplayAddress(action.address))
                } else {
                    state.asReducerResult()
                }
            }
            is MapClicked -> {
                when (state) {
                    is Initial -> state
                    is AutocompleteIsActive -> MapIsActive(
                        state.mapLocationState,
                        LocationSelected(action.latLng, action.address)
                    )
                    is MapIsActive -> state
                    is Confirmed -> state
                }.withEffects(
                    CloseKeyboard,
                    RemoveSearchFocus,
                    DisplayAddress(action.address)
                )
            }
            is SearchQueryChanged -> {
                reduce(action, state).asReducerResult()
            }
            is AutocompleteError -> {
                when (state) {
                    is AutocompleteIsActive -> reduce(action, state, state.mapLocationState)
                    is Initial -> reduce(action, state, state.mapLocationState)
                    is MapIsActive -> reduce(action, state, state.mapLocationState)
                    is Confirmed -> throw IllegalActionException(action, state)
                }.withEffects(CloseKeyboard)
            }
            ConfirmClicked -> {
                when (state) {
                    is AutocompleteIsActive -> state.asReducerResult()
                    is Initial -> state.asReducerResult()
                    is MapIsActive -> state.mapLocationState.map.let {
                        if (it is MapReady) {
                            Confirmed(state.placeData, state)
                                .withEffects(
                                    Proceed(state.placeData),
                                    CloseKeyboard
                                )
                        } else state.asReducerResult()
                    }
                    is Confirmed -> state.asReducerResult()
                }
            }
            is PlaceSelectedAction -> {
                when (state) {
                    is AutocompleteIsActive -> {
                        when (val map = state.mapLocationState.map) {
                            is MapReady -> {
                                MapIsActive(
                                    state.mapLocationState, PlaceSelected(
                                        displayAddress = action.displayAddress,
                                        strictAddress = action.strictAddress,
                                        name = action.name,
                                        latLng = action.latLng
                                    )
                                ).withEffects(
                                    CloseKeyboard,
                                    RemoveSearchFocus,
                                    MoveMap(action.latLng, map.mapWrapper)
                                )
                            }
                            else -> throw IllegalActionException(action, state)
                        }
                    }
                    is Initial, is MapIsActive, is Confirmed -> throw IllegalActionException(
                        action,
                        state
                    )
                }
            }
            Reset -> {
                when (state) {
                    is Confirmed -> state.lastState
                    else -> throw IllegalActionException(action, state)
                }.asReducerResult()
            }
        }
    }

    private fun reduce(
        action: AutocompleteError,
        state: State,
        mapLocationState: MapLocationState
    ): State {
        return when (val map = mapLocationState.map) {
            is MapReady -> {
                MapIsActive(
                    mapLocationState,
                    LocationSelectedWithUnsuccesfulQuery(map, action.query)
                )
            }
            MapNotReady -> state
        }
    }

    private fun reduce(action: SearchQueryChanged, state: State): State {
        return when (state) {
            is Initial -> AutocompleteIsActive(
                state.mapLocationState,
                action.query,
                action.results
            )
            is AutocompleteIsActive -> AutocompleteIsActive(
                state.mapLocationState,
                action.query,
                action.results
            )
            is MapIsActive -> AutocompleteIsActive(
                state.mapLocationState,
                action.query,
                action.results
            )
            is Confirmed -> state
        }
    }

    private fun reduce(action: MapCameraMoved, state: MapLocationState): MapLocationState {
        return when (state.map) {
            MapNotReady -> throw IllegalActionException(action, state)
            is MapReady -> {
                state.withPosition(state.map, action.latLng)
            }
        }
    }

    private fun reduce(action: UserLocation, state: State): ReducerResult {
        return when (state) {
            is Initial -> state.withUserLocation(action.latLng).withEffects(
                if (state.mapLocationState.map is MapReady) {
                    getMapMoveEffectIfNeeded(
                        state.mapLocationState,
                        state.mapLocationState.map,
                        action.latLng
                    )
                } else setOf()
            )
            is MapIsActive -> state.withUserLocation(action.latLng).asReducerResult()
            is AutocompleteIsActive -> state.withUserLocation(action.latLng).asReducerResult()
            is Confirmed -> state.asReducerResult()
        }
    }

    private fun getMapMoveEffectIfNeeded(
        it: MapLocationState,
        map: MapReady,
        userLocation: LatLng?
    ): Set<Effect> {
        return if (it.initialMovePerformed is False && userLocation != null) {
            setOf(createMapMoveEffect(map, userLocation, it.initialMovePerformed))
        } else {
            setOf()
        }
    }

    private fun createMapMoveEffect(
        map: MapReady,
        userLocation: LatLng,
        _initialMovePerformed: False
    ): MoveMap {
        return MoveMap(userLocation, map.mapWrapper)
    }

    companion object {
        val INITIAL_STATE = Initial(
            MapLocationState(
                MapNotReady, null, initialMovePerformed = False
            ),
            null
        )
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

private fun MapLocationState.withPosition(mapReady: MapReady, latLng: LatLng): MapLocationState {
    return copy(map = mapReady.copy(cameraPosition = latLng))
}

