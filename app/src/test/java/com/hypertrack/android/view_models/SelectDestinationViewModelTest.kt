package com.hypertrack.android.view_models

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.MainCoroutineScopeRule
import com.hypertrack.android.ui.common.select_destination.reducer.*
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SelectDestinationViewModelTest {

    @ExperimentalCoroutinesApi
    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()

    @Test
    fun init() {
        val reducer = SelectDestinationViewModelReducer(mockk(relaxed = true), TestCoroutineScope())

        runBlocking {
            var state: State = Initial(MapLocationState(MapNotReady, null, false), null)
            println(state)
            state = reducer.sendAction(state, UserLocation(LatLng(37.4, -122.0)))
            state =
                reducer.sendAction(state, MapReadyAction(mockk(relaxed = true), LatLng(0.0, 0.0)))
            state = reducer.sendAction(state, MapCameraMoved(LatLng(0.01, 0.0), false))
        }

    }

    private fun SelectDestinationViewModelReducer.sendAction(
        oldState: State,
        action: Action
    ): State {
        println("action = ${action}")
        val newState = reduceAction(oldState, action)
        println("new state = ${newState}")
        return newState
    }

}