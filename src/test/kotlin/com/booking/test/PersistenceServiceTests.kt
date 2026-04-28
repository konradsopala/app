package com.booking.test

import com.booking.model.Booking
import com.booking.model.Quote
import com.booking.model.User
import com.booking.service.BookingService
import com.booking.service.PersistenceService
import com.booking.service.UserService
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

private fun freshTempDir(): File {
    val dir = File.createTempFile("booking-test-", "")
    dir.delete()
    dir.mkdirs()
    dir.deleteOnExit()
    return dir
}

fun runPersistenceServiceTests() = TestRunner.suite("PersistenceService") {

    TestRunner.test("round-trip preserves bookings, quote, audit, users, capacity") {
        val dir = freshTempDir()

        // Original world
        val s1 = BookingService()
        val u1 = UserService()
        val p1 = PersistenceService(dir, s1, u1)
        s1.currentActor = "alice"

        u1.addUser("alice", "alicepw", User.Role.ADMIN)
        u1.addUser("bob", "bobpass", User.Role.USER)

        val a = s1.createBooking("Alice", LocalDate.now().plusDays(30), LocalTime.of(10, 0), 60, "Coffee")
        val b = s1.createBooking("Bob", LocalDate.now().plusDays(31), LocalTime.of(14, 30), 90, "Lunch")
        s1.attachQuote(a.id, Quote(123.45, "VIP", 4, 5, "SAVE10", true, "MID"))
        s1.cancelBooking(b.id)
        s1.capacity = 3
        p1.save()

        // New world reloads from disk
        val s2 = BookingService()
        val u2 = UserService()
        val p2 = PersistenceService(dir, s2, u2)
        p2.load()

        assertEquals(3, s2.capacity, "capacity should round-trip")
        val bookings = s2.listBookings()
        assertEquals(2, bookings.size)

        val ra = s2.findBooking(a.id)!!
        assertEquals("Alice", ra.customerName)
        assertEquals(60, ra.durationMinutes)
        assertEquals(LocalTime.of(10, 0), ra.startTime)
        assertEquals(Booking.Status.CONFIRMED, ra.status)
        assertNotNull(ra.quote)
        assertEquals(123.45, ra.quote!!.total)
        assertEquals("VIP", ra.quote!!.customerType)
        assertEquals(4, ra.quote!!.partySize)

        val rb = s2.findBooking(b.id)!!
        assertEquals(Booking.Status.CANCELLED, rb.status)
        assertNull(rb.quote)

        val users = u2.listUsers()
        assertEquals(2, users.size)
        assertNotNull(u2.login("alice", "alicepw"))
        assertNotNull(u2.login("bob", "bobpass"))

        val audit = s2.auditLog.getAll()
        assertTrue(audit.isNotEmpty())
        assertTrue(audit.any { it.action.name == "CREATED" && it.actor == "alice" })
        assertTrue(audit.any { it.action.name == "QUOTED" })
        assertTrue(audit.any { it.action.name == "CANCELLED" })
    }

    TestRunner.test("load on empty directory is a no-op") {
        val dir = freshTempDir()
        val s = BookingService()
        val u = UserService()
        val p = PersistenceService(dir, s, u)
        p.load() // should not throw
        assertEquals(0, s.listBookings().size)
        assertEquals(0, u.listUsers().size)
    }

    TestRunner.test("escaped customer name with tab and newline survives round-trip") {
        val dir = freshTempDir()
        val s1 = BookingService()
        val u1 = UserService()
        val p1 = PersistenceService(dir, s1, u1)
        val tricky = "Weird\tName\nWith\\backslash"
        val b = s1.createBooking(tricky, LocalDate.now().plusDays(30), LocalTime.of(10, 0), 60, "x")
        p1.save()

        val s2 = BookingService()
        val p2 = PersistenceService(dir, s2, UserService())
        p2.load()
        assertEquals(tricky, s2.findBooking(b.id)?.customerName)
    }
}
