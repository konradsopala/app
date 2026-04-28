package com.booking.test

import com.booking.service.BookingPricer
import com.booking.service.BookingService
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

private fun nextOccurrence(target: DayOfWeek): LocalDate {
    var d = LocalDate.now().plusDays(7)
    while (d.dayOfWeek != target) d = d.plusDays(1)
    return d
}

private fun silent(block: () -> Unit) {
    val original = System.out
    System.setOut(PrintStream(ByteArrayOutputStream()))
    try { block() } finally { System.setOut(original) }
}

fun runBookingPricerTests() = TestRunner.suite("BookingPricer") {

    TestRunner.test("pricing attaches Quote to booking") {
        val s = BookingService()
        val p = BookingPricer(s)
        val b = s.createBooking("Alice", LocalDate.now().plusDays(30), LocalTime.of(10, 0), 60, "x")
        silent {
            p.calculateAndPrintAndMaybeSave(b.id, "REGULAR", 2, 0, null, false, "MID", null)
        }
        val q = s.findBooking(b.id)?.quote
        assertTrue(q != null, "expected quote attached")
        assertEquals("REGULAR", q!!.customerType)
        assertEquals(2, q.partySize)
        assertTrue(q.total > 0.0)
    }

    TestRunner.test("invalid customerType throws") {
        val s = BookingService()
        val p = BookingPricer(s)
        val b = s.createBooking("Alice", LocalDate.now().plusDays(30), LocalTime.of(10, 0), 60, "x")
        assertThrows<IllegalArgumentException> {
            silent { p.calculateAndPrintAndMaybeSave(b.id, "BOGUS", 2, 0, null, false, "MID", null) }
        }
    }

    TestRunner.test("VIP weekend HIGH applies multiplier and discount") {
        val s = BookingService()
        val p = BookingPricer(s)
        val saturday = nextOccurrence(DayOfWeek.SATURDAY)
        val b = s.createBooking("Alice", saturday, LocalTime.of(10, 0), 60, "x")
        val total = silent {} .let {
            // run pricing under captured stdout, return total from second call
            var t = 0.0
            silent {
                t = p.calculateAndPrintAndMaybeSave(b.id, "VIP", 1, 0, null, false, "HIGH", null)
            }
            t
        }
        // VIP base 200 × 1.5 (weekend HIGH) × 0.95 (VIP weekend bonus) = 285.0; days >60 from now is unlikely, leave > base
        assertTrue(total >= 285.0 - 0.01, "expected at least 285, got $total")
    }
}
