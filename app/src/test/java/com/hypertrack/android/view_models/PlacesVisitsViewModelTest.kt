package com.hypertrack.android.view_models

import com.hypertrack.android.ui.screens.visits_management.tabs.places.VisitItem
import com.hypertrack.android.ui.screens.visits_management.tabs.places.VisitsData
import com.hypertrack.android.ui.screens.visits_management.tabs.places.getDay
import com.hypertrack.android.utils.MockData
import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.Month

class PlacesVisitsViewModelTest {

    @Test
    fun testAdapterData() {
        val now = LocalDate.now()
        val visits = listOf(
            MockData.createGeofenceVisit(now),
            MockData.createGeofenceVisit(now),
            MockData.createGeofenceVisit(now.minusDays(1)),
            MockData.createGeofenceVisit(now.minusDays(40)),
            MockData.createGeofenceVisit(now.minusDays(41)),
        )
        val data = VisitsData(visits, mutableMapOf<LocalDate, Int>().apply {
            visits.forEach { put(it.getDay(), 100) }
        })
        val adapterData = data.adapterData
        //todo
//        assertEquals(listOf<VisitItem>(), adapterData)
//        assertEquals(listOf<VisitItem>(), data.monthStats.keys)
    }

}