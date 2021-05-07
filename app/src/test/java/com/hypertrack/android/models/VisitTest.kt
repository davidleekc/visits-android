package com.hypertrack.android.models

import com.hypertrack.android.api.Point
import com.hypertrack.android.api.Trip
import com.hypertrack.android.api.TripDestination
import com.hypertrack.android.api.Views
import com.hypertrack.android.models.local.TripStatus
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class VisitTest {
    @Test
    fun `it should not delete local Visits`() {
        val localVisit =
            Visit("42", "42", visitType = VisitType.LOCAL, _state = VisitStatus.VISITED)
        assertFalse(localVisit.isDeletable)
    }

    @Test
    fun `it should not delete trips completed during the last day`() {
        val completedAt = Instant.now().minusSeconds(3600).toString()
//        println("Completed at $completedAt")
        val recentTrip = Visit(
            "42",
            "42",
            completedAt = completedAt,
            visitType = VisitType.TRIP,
            _state = VisitStatus.COMPLETED
        )
        assertFalse(recentTrip.isDeletable)
    }

    @Test
    fun `it should delete trips completed earlier than the the last day`() {
        val completedAt = Instant.now().minus(2, ChronoUnit.DAYS).toString()
//        println("Completed at $completedAt")

        val recentTrip = Visit(
            "42",
            "42",
            completedAt = completedAt,
            visitType = VisitType.TRIP,
            _state = VisitStatus.COMPLETED
        )
        assertTrue(recentTrip.isDeletable)
    }

    @Test
    fun `it should automatically check in pending if arrival time is present in prototype`() {
        val createdAt = "2020-02-02T20:02:02.020Z"
        val tripId = "42"
        val pending = Visit(
            tripId,
            createdAt = createdAt,
            visitType = VisitType.TRIP,
            _state = VisitStatus.PENDING
        )
        val prototype: VisitDataSource = Trip(
            tripId = tripId,
            status = TripStatus.ACTIVE.value,
            orders = null,
            views = Views("null", null),
            createdAt = createdAt,
            metadata = emptyMap(), destination = TripDestination(
                null,
                Point(listOf(42.0, 42.0)),
                arrivedAt = "2020-02-02T20:20:02.020Z"
            ),
            estimate = Estimate("2020-02-02T20:20:02.020Z", null)
        )
        val updated = pending.update(prototype)
        assertEquals(VisitStatus.VISITED, updated.state)
    }

    @Test
    fun `it should automatically check in picked up if arrival time is present in prototype`() {
        val createdAt = "2020-02-02T20:02:02.020Z"
        val tripId = "42"
        val pending = Visit(
            tripId,
            createdAt = createdAt,
            visitType = VisitType.TRIP,
            _state = VisitStatus.PICKED_UP
        )
        val prototype: VisitDataSource = Trip(
            tripId = tripId,
            status = TripStatus.ACTIVE.value,
            orders = null,
            views = Views("null", null), createdAt = createdAt,
            metadata = emptyMap(), destination = TripDestination(
                null,
                Point(listOf(42.0, 42.0)),
                arrivedAt = "2020-02-02T20:20:02.020Z"
            ),
            estimate = Estimate("2020-02-02T20:20:02.020Z", null)
        )
        val updated = pending.update(prototype)
        assertEquals(VisitStatus.VISITED, updated.state)
    }


    @Test
    fun `it should update arrival in pending if arrival time is present in prototype`() {
        val createdAt = "2020-02-02T20:02:02.020Z"
        val tripId = "42"
        val pending = Visit(
            tripId,
            createdAt = createdAt,
            visitType = VisitType.TRIP,
            _state = VisitStatus.PENDING
        )
        val arrivedAt = "2020-02-02T20:20:02.020Z"
        val prototype: VisitDataSource = Trip(
            tripId = tripId,
            status = TripStatus.ACTIVE.value,
            orders = null,
            views = Views("", null), createdAt = createdAt,
            metadata = emptyMap(), destination = TripDestination(
                null,
                Point(listOf(42.0, 42.0)),
                arrivedAt = arrivedAt
            ),
            estimate = Estimate("2020-02-02T20:20:02.020Z", null)
        )
        val updated = pending.update(prototype)
        assertEquals(arrivedAt, updated.visitedAt)
    }

    @Test
    fun `it should update arrival in picked up if arrival time is present in prototype`() {
        val createdAt = "2020-02-02T20:02:02.020Z"
        val tripId = "42"
        val pending = Visit(
            tripId,
            createdAt = createdAt,
            visitType = VisitType.TRIP,
            _state = VisitStatus.PICKED_UP
        )
        val arrivedAt = "2020-02-02T20:20:02.020Z"
        val prototype: VisitDataSource = Trip(
            tripId = tripId,
            status = TripStatus.ACTIVE.value,
            orders = null,
            views = Views("", null), createdAt = createdAt,
            metadata = emptyMap(), destination = TripDestination(
                null,
                Point(listOf(42.0, 42.0)),
                arrivedAt = arrivedAt
            ),
            estimate = Estimate("2020-02-02T20:20:02.020Z", null)
        )
        val updated = pending.update(prototype)
        assertEquals(arrivedAt, updated.visitedAt)
    }

    @Test
    fun `it should keep notes in pending if arrival time is present in prototype`() {
        val createdAt = "2020-02-02T20:02:02.020Z"
        val tripId = "42"
        val pending = Visit(
            tripId, createdAt = createdAt, visitType = VisitType.TRIP,
            visitNote = "important Note", _state = VisitStatus.PENDING
        )
        val arrivedAt = "2020-02-02T20:20:02.020Z"
        val prototype: VisitDataSource = Trip(
            tripId = tripId,
            status = TripStatus.ACTIVE.value,
            orders = null,
            views = Views("", null), createdAt = createdAt,
            metadata = emptyMap(), destination = TripDestination(
                null,
                Point(listOf(42.0, 42.0)),
                arrivedAt = arrivedAt
            ),
            estimate = Estimate("2020-02-02T20:20:02.020Z", null)
        )
        val updated = pending.update(prototype)
        assertEquals(pending.visitNote, updated.visitNote)
    }

    @Test
    fun `it should keep notes in picked up if arrival time is present in prototype`() {
        val createdAt = "2020-02-02T20:02:02.020Z"
        val tripId = "42"
        val pending = Visit(
            tripId, createdAt = createdAt, visitType = VisitType.TRIP, visitNote = "important Note",
            _state = VisitStatus.PICKED_UP
        )
        val arrivedAt = "2020-02-02T20:20:02.020Z"
        val prototype: VisitDataSource = Trip(
            tripId = tripId,
            status = TripStatus.ACTIVE.value,
            orders = null,
            views = Views("", null), createdAt = createdAt,
            metadata = emptyMap(), destination = TripDestination(
                null,
                Point(listOf(42.0, 42.0)),
                arrivedAt = arrivedAt
            ),
            estimate = Estimate("2020-02-02T20:20:02.020Z", null)
        )
        val updated = pending.update(prototype)
        assertEquals(pending.visitNote, updated.visitNote)
    }
}