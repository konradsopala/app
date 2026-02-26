package com.booking.service

import com.booking.model.Booking
import java.io.FileWriter
import java.io.PrintWriter
import java.time.LocalDate

/**
 * Core service handling all booking CRUD operations and business logic.
 *
 * Backed by in-memory storage with insertion-order preservation.
 * All mutations are recorded in the embedded [AuditLog].
 */
class BookingService {

    private val bookings = linkedMapOf<String, Booking>()
    val auditLog = AuditLog()

    // ── Create ──────────────────────────────────────────────────────

    fun createBooking(customerName: String, date: LocalDate, description: String): Booking {
        require(!date.isBefore(LocalDate.now())) { "Booking date cannot be in the past." }
        val booking = Booking(customerName, date, description)
        bookings[booking.id] = booking
        auditLog.log(booking.id, AuditLog.Action.CREATED,
            "Customer: $customerName, Date: $date")
        return booking
    }

    // ── Cancel ──────────────────────────────────────────────────────

    fun cancelBooking(id: String): Boolean {
        val booking = bookings[id] ?: return false
        if (booking.status == Booking.Status.CANCELLED) return false
        booking.cancel()
        auditLog.log(id, AuditLog.Action.CANCELLED, "Cancelled by user")
        return true
    }

    // ── Find ────────────────────────────────────────────────────────

    fun findBooking(id: String): Booking? = bookings[id]

    // ── List ────────────────────────────────────────────────────────

    fun listBookings(): List<Booking> = bookings.values.toList()

    // ── Search by customer name (case-insensitive, partial match) ──

    fun searchByCustomer(name: String): List<Booking> {
        val lowerName = name.lowercase()
        return bookings.values.filter { it.customerName.lowercase().contains(lowerName) }
    }

    // ── Update / reschedule ─────────────────────────────────────────

    fun updateBooking(id: String, newDate: LocalDate?, newDescription: String?): Booking {
        val booking = bookings[id]
            ?: throw IllegalArgumentException("Booking not found.")
        check(booking.status != Booking.Status.CANCELLED) { "Cannot update a cancelled booking." }
        if (newDate != null) {
            require(!newDate.isBefore(LocalDate.now())) { "New date cannot be in the past." }
            booking.date = newDate
        }
        if (!newDescription.isNullOrBlank()) {
            booking.description = newDescription
        }
        auditLog.log(id, AuditLog.Action.UPDATED,
            "Date: ${booking.date}, Desc: ${booking.description}")
        return booking
    }

    // ── Statistics ──────────────────────────────────────────────────

    fun getStatistics(): Map<String, Long> {
        val total = bookings.size.toLong()
        val confirmed = bookings.values.count { it.status == Booking.Status.CONFIRMED }.toLong()
        val cancelled = bookings.values.count { it.status == Booking.Status.CANCELLED }.toLong()
        return linkedMapOf("total" to total, "confirmed" to confirmed, "cancelled" to cancelled)
    }

    // ── Export to CSV ───────────────────────────────────────────────

    fun exportToCsv(filePath: String) {
        PrintWriter(FileWriter(filePath)).use { writer ->
            writer.println("id,customer,date,description,status")
            for (b in bookings.values) {
                writer.printf(
                    "%s,%s,%s,%s,%s%n",
                    escape(b.id), escape(b.customerName),
                    b.date, escape(b.description), b.status
                )
            }
        }
        auditLog.log("SYSTEM", AuditLog.Action.EXPORTED, "Exported to $filePath")
    }

    private fun escape(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
