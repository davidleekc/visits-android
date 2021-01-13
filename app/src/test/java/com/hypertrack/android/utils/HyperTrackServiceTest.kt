package com.hypertrack.android.utils

import com.hypertrack.android.models.Visit
import com.hypertrack.android.models.VisitStatus
import com.hypertrack.android.models.VisitType
import com.hypertrack.sdk.HyperTrack
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertTrue
import org.junit.Test

class HyperTrackServiceTest {

    @Test
    fun `it should attach visit note to completion geotag`() {

        val sdk = mockk<HyperTrack>(relaxed = true)
        val listener = TrackingState()

        val hyperTrackService = HyperTrackService(listener, sdk)
        val visitNote = "valuable customer Note"
        val visit = Visit(_id = "42", visitNote = visitNote, visitType = VisitType.LOCAL, _state = VisitStatus.VISITED)

        val slot = slot<Map<String, Any>>()
        every { sdk.addGeotag(capture(slot)) } returns sdk
        hyperTrackService.sendCompletionEvent(visit._id, visit.visitNote, visit.typeKey, true)

        val payload = slot.captured
        assertTrue(payload.isNotEmpty())
        assertTrue(payload.containsKey("visit_note"))
        assertTrue(payload["visit_note"] == visitNote)
    }
}

