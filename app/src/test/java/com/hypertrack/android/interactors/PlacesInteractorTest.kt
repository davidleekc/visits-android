package com.hypertrack.android.interactors

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.VisibleRegion
import com.hypertrack.android.observeAndGetValue
import io.mockk.mockk
import junit.framework.TestCase
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.Test

@ExperimentalCoroutinesApi
class PlacesInteractorTest {

    @Test
    fun `it should load geofences for geohash with pagination`() {
        val placesInteractor = PlacesInteractorImpl(
            mockk(relaxed = true),
            mockk(relaxed = true),
            TestCoroutineScope(),
        )

        runBlocking {
            placesInteractor.loadGeofencesForMap(LatLng(0.0, 0.0))
        }

        runBlocking {
            assertEquals(10, placesInteractor.geofences.observeAndGetValue().size)
        }

    }

}