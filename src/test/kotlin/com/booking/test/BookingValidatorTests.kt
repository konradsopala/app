package com.booking.test

import com.booking.service.BookingService
import com.booking.service.BookingValidator
import java.time.LocalDate
import java.time.LocalTime

private fun future(daysAhead: Long = 30) = LocalDate.now().plusDays(daysAhead)

fun runBookingValidatorTests() = TestRunner.suite("BookingValidator") {

    TestRunner.test("valid input passes") {
        val s = BookingService()
        val v = BookingValidator(s)
        val r = v.validateNewBooking("Alice", future(), LocalTime.of(10, 0), 60, "Tea")
        assertTrue(r.valid, r.errors.toString())
    }

    TestRunner.test("blank fields are reported") {
        val s = BookingService()
        val v = BookingValidator(s)
        val r = v.validateNewBooking("", future(), LocalTime.of(10, 0), 60, "")
        assertFalse(r.valid)
        assertTrue(r.errors.any { it.contains("Customer name") })
        assertTrue(r.errors.any { it.contains("Description") })
    }

    TestRunner.test("zero duration rejected") {
        val s = BookingService()
        val v = BookingValidator(s)
        val r = v.validateNewBooking("Alice", future(), LocalTime.of(10, 0), 0, "Tea")
        assertFalse(r.valid)
        assertTrue(r.errors.any { it.contains("Duration") })
    }

    TestRunner.test("crossing midnight rejected") {
        val s = BookingService()
        val v = BookingValidator(s)
        val r = v.validateNewBooking("Alice", future(), LocalTime.of(23, 30), 60, "Tea")
        assertFalse(r.valid)
        assertTrue(r.errors.any { it.contains("midnight") || it.contains("same day") })
    }

    TestRunner.test("capacity overlap rejected at default 1") {
        val s = BookingService()
        val v = BookingValidator(s)
        s.createBooking("Alice", future(), LocalTime.of(10, 0), 60, "x")
        val r = v.validateNewBooking("Bob", future(), LocalTime.of(10, 30), 60, "y")
        assertFalse(r.valid)
        assertTrue(r.errors.any { it.contains("Time slot is full") })
    }

    TestRunner.test("capacity overlap allowed when bumped") {
        val s = BookingService().also { it.capacity = 2 }
        val v = BookingValidator(s)
        s.createBooking("Alice", future(), LocalTime.of(10, 0), 60, "x")
        val r = v.validateNewBooking("Bob", future(), LocalTime.of(10, 30), 60, "y")
        assertTrue(r.valid, r.errors.toString())
    }

    TestRunner.test("update validation excludes self when rescheduling") {
        val s = BookingService()
        val v = BookingValidator(s)
        val a = s.createBooking("Alice", future(), LocalTime.of(10, 0), 60, "x")
        // Reschedule a within its own slot — must pass even at capacity 1.
        val r = v.validateUpdate(a.id, null, LocalTime.of(10, 30), null)
        assertTrue(r.valid, r.errors.toString())
    }

    TestRunner.test("duplicate booking same customer same day blocked") {
        val s = BookingService()
        val v = BookingValidator(s)
        s.createBooking("Alice", future(), LocalTime.of(10, 0), 60, "x")
        val r = v.validateNewBooking("alice", future(), LocalTime.of(15, 0), 60, "y")
        assertFalse(r.valid)
        assertTrue(r.errors.any { it.contains("already has a booking") })
    }
}
