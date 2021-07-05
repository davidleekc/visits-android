package com.hypertrack.android.models

import com.hypertrack.android.api.Point
import com.hypertrack.android.api.TripDestination
import com.hypertrack.android.createBaseOrder
import com.hypertrack.android.models.local.LocalOrder
import com.hypertrack.android.ui.common.delegates.OrderAddressDelegate
import com.hypertrack.android.ui.common.formatDateTime
import com.hypertrack.logistics.android.github.R
import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import org.junit.Test

class LocalOrderTest {

    @Test
    fun `it should deserialize order metadata`() {
        Metadata.deserialize(
            mapOf(
                "visits_app" to mapOf(
                    "note" to "Note",
                    "photos" to listOf("1", "2"),
                    "appended" to mapOf(
                        "1" to mapOf(
                            "note" to "Note1",
                            "photos" to listOf("1", "2", "3")
                        ),
                        "2" to mapOf(
                            "note" to "Note2",
                            "photos" to listOf("1", "2", "3", "4")
                        )
                    )
                ),
                "other" to "data"
            )
        ).let {
            assertEquals("data", it.otherMetadata["other"])
            assertEquals("Note", it.visitsAppMetadata.note)
            assertEquals(2, it.visitsAppMetadata.photos!!.size)
            assertEquals("Note1", it.visitsAppMetadata.appended!!["1"]!!.note)
            assertEquals(3, it.visitsAppMetadata.appended!!["1"]!!.photos!!.size)
            assertEquals("Note2", it.visitsAppMetadata.appended!!["2"]!!.note)
            assertEquals(4, it.visitsAppMetadata.appended!!["2"]!!.photos!!.size)
        }
    }

}