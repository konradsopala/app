package com.booking.test

import com.booking.model.Booking
import com.booking.service.BookingService
import java.time.LocalDate
import java.time.LocalTime

private fun future(daysAhead: Long = 30) = LocalDate.now().plusDays(daysAhead)

fun runBookingServiceTests() = TestRunner.suite("BookingService") {

    TestRunner.test("createBooking stores booking and audits CREATED") {
        val s = BookingService()
        s.currentActor = "alice"
        val b = s.createBooking("Bob", future(), LocalTime.of(10, 0), 60, "Lunch")
        assertEquals(1, s.listBookings().size)
        assertEquals(b.id, s.findBooking(b.id)?.id)
        val entries = s.auditLog.getAll()
        assertEquals(1, entries.size)
        assertEquals("alice", entries.first().actor)
    }

    TestRunner.test("createBooking rejects past date") {
        val s = BookingService()
        assertThrows<IllegalArgumentException> {
            s.createBooking("Bob", LocalDate.now().minusDays(1), LocalTime.of(10, 0), 60, "Past")
        }
    }

    TestRunner.test("cancelBooking flips status and is idempotent") {
        val s = BookingService()
        val b = s.createBooking("Bob", future(), LocalTime.of(10, 0), 60, "Lunch")
        assertTrue(s.cancelBooking(b.id))
        assertEquals(Booking.Status.CANCELLED, s.findBooking(b.id)!!.status)
        assertFalse(s.cancelBooking(b.id), "second cancel should return false")
    }

    TestRunner.test("overlappingBookings finds same-day overlaps and respects exclude") {
        val s = BookingService()
        val a = s.createBooking("A", future(), LocalTime.of(10, 0), 60, "x")
        s.createBooking("B", future(), LocalTime.of(11, 30), 60, "y")

        val overlap = s.overlappingBookings(future(), LocalTime.of(10, 30), 60)
        assertEquals(1, overlap.size, "expected only A to overlap 10:30-11:30")

        val excluded = s.overlappingBookings(future(), LocalTime.of(10, 30), 60, excludeId = a.id)
        assertEquals(0, excluded.size, "exclude should drop A")
    }

    TestRunner.test("overlap excludes cancelled bookings") {
        val s = BookingService()
        val a = s.createBooking("A", future(), LocalTime.of(10, 0), 60, "x")
        s.cancelBooking(a.id)
        val overlap = s.overlappingBookings(future(), LocalTime.of(10, 30), 60)
        assertEquals(0, overlap.size)
    }

    TestRunner.test("onChange fires after create/cancel/update/quote/capacity") {
        val s = BookingService()
        var calls = 0
        s.onChange = { calls++ }
        val b = s.createBooking("A", future(), LocalTime.of(10, 0), 60, "x")
        s.updateBooking(b.id, null, LocalTime.of(11, 0), null, null)
        s.cancelBooking(b.id)
        s.capacity = 2
        assertEquals(4, calls)
    }

    TestRunner.test("totalQuotedRevenue sums confirmed quotes only") {
        val s = BookingService()
        val a = s.createBooking("A", future(), LocalTime.of(10, 0), 60, "x")
        val b = s.createBooking("B", future(), LocalTime.of(12, 0), 60, "y")
        s.attachQuote(a.id, com.booking.model.Quote(100.0, "REGULAR", 2, 0, null, false, "MID"))
        s.attachQuote(b.id, com.booking.model.Quote(50.0, "REGULAR", 1, 0, null, false, "MID"))
        s.cancelBooking(b.id)
        assertEquals(100.0, s.totalQuotedRevenue())
    }
}
