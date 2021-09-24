package com.hypertrack.android.interactors

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.VisibleRegion
import com.hypertrack.android.api.ApiClient
import com.hypertrack.android.createBaseTrip
import com.hypertrack.android.models.GeofenceMetadata
import com.hypertrack.android.observeAndGetValue
import com.hypertrack.android.repository.PlacesRepositoryImpl
import com.hypertrack.android.utils.Injector
import com.hypertrack.android.utils.MockData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyAll
import io.mockk.mockk
import junit.framework.Assert
import junit.framework.TestCase
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.Test
import retrofit2.Response

@ExperimentalCoroutinesApi
class PlacesInteractorTest {

    @Test
    fun `it should create a geofence with null address if address is blank`() {
        var slot: GeofenceMetadata = GeofenceMetadata(address = "default")
        val apiClient: ApiClient = mockk(relaxed = true) {
            coEvery { createGeofence(any(), any(), any(), any()) } coAnswers {
                slot = arg(3)
                Response.success(listOf(MockData.createGeofence()))
            }
        }
        val placesInteractor = PlacesInteractorImpl(
            PlacesRepositoryImpl(
                "1",
                apiClient,
                Injector.getMoshi(),
                mockk(relaxed = true),
                mockk(relaxed = true),
            ),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            TestCoroutineScope(),
        )
        runBlocking {
            placesInteractor.createGeofence(
                1.1, 1.1, null,
                " ",
                100, null, null
            )
        }
        coVerify {
            apiClient.createGeofence(any(), any(), any(), any())
        }
        assertEquals(null, slot.address)
    }

}