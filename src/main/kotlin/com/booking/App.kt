package com.booking

import com.booking.model.Booking
import com.booking.service.AuditLog
import com.booking.service.BookingPricer
import com.booking.service.BookingService
import com.booking.service.BookingValidator
import com.booking.service.ReportGenerator
import com.booking.util.BookingFilter
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException
import java.util.Scanner

fun main() {
    App().run()
}

class App {

    private val service = BookingService()
    private val validator = BookingValidator(service)
    private val reportGenerator = ReportGenerator(service)
    private val pricer = BookingPricer(service)
    private val scanner = Scanner(System.`in`)

    fun run() {
        println("=== Booking Manager v2 ===")

        while (true) {
            println("""
                |
                | 1) Create booking
                | 2) List bookings
                | 3) Find booking
                | 4) Cancel booking
                | 5) Search by customer
                | 6) Update booking
                | 7) Statistics
                | 8) Export to CSV
                | 9) Generate report
                |10) Advanced search
                |11) View audit log
                |12) Booking history
                |13) Quote price
                |14) Set capacity (current: ${service.capacity})
                |15) Exit
            """.trimMargin())
            print("\nChoice: ")

            when (scanner.nextLine().trim()) {
                "1"  -> createBooking()
                "2"  -> listBookings()
                "3"  -> findBooking()
                "4"  -> cancelBooking()
                "5"  -> searchByCustomer()
                "6"  -> updateBooking()
                "7"  -> showStatistics()
                "8"  -> exportToCsv()
                "9"  -> generateReport()
                "10" -> advancedSearch()
                "11" -> viewAuditLog()
                "12" -> viewBookingHistory()
                "13" -> quotePrice()
                "14" -> setCapacity()
                "15" -> { println("Goodbye!"); return }
                else -> println("Invalid choice.")
            }
        }
    }

    // ── 1. Create ──────────────────────────────────────────────────

    private fun createBooking() {
        print("Customer name: ")
        val name = scanner.nextLine().trim()

        print("Date (YYYY-MM-DD): ")
        val date: LocalDate
        try {
            date = LocalDate.parse(scanner.nextLine().trim())
        } catch (e: DateTimeParseException) {
            println("Invalid date format."); return
        }

        print("Start time (HH:MM, 24h): ")
        val startTime: LocalTime
        try {
            startTime = LocalTime.parse(scanner.nextLine().trim())
        } catch (e: DateTimeParseException) {
            println("Invalid time format."); return
        }

        print("Duration in minutes: ")
        val duration = scanner.nextLine().trim().toIntOrNull()
        if (duration == null || duration <= 0) {
            println("Duration must be a positive integer."); return
        }

        print("Description: ")
        val description = scanner.nextLine().trim()

        val result = validator.validateNewBooking(name, date, startTime, duration, description)
        if (!result.valid) {
            println("Validation failed:")
            result.errors.forEach { println("  - $it") }
            return
        }

        try {
            val booking = service.createBooking(name, date, startTime, duration, description)
            println("Booking created: $booking")
        } catch (e: IllegalArgumentException) {
            println("Error: ${e.message}")
        }
    }

    // ── 2. List ────────────────────────────────────────────────────

    private fun listBookings() {
        val bookings = service.listBookings()
        if (bookings.isEmpty()) { println("No bookings found."); return }
        bookings.forEach(::println)
    }

    // ── 3. Find ────────────────────────────────────────────────────

    private fun findBooking() {
        print("Booking ID: ")
        val id = scanner.nextLine().trim()
        val booking = service.findBooking(id)
        if (booking == null) println("Booking not found.") else println(booking)
    }

    // ── 4. Cancel ──────────────────────────────────────────────────

    private fun cancelBooking() {
        print("Booking ID to cancel: ")
        val id = scanner.nextLine().trim()
        if (service.cancelBooking(id)) println("Booking cancelled.")
        else println("Booking not found or already cancelled.")
    }

    // ── 5. Search by customer ──────────────────────────────────────

    private fun searchByCustomer() {
        print("Customer name to search: ")
        val name = scanner.nextLine().trim()
        if (name.isEmpty()) { println("Search term cannot be empty."); return }
        val results = service.searchByCustomer(name)
        if (results.isEmpty()) {
            println("No bookings found for \"$name\".")
        } else {
            println("Found ${results.size} booking(s):")
            results.forEach(::println)
        }
    }

    // ── 6. Update / reschedule ─────────────────────────────────────

    private fun updateBooking() {
        print("Booking ID to update: ")
        val id = scanner.nextLine().trim()

        print("New date (YYYY-MM-DD, leave blank to keep): ")
        val dateInput = scanner.nextLine().trim()
        var newDate: LocalDate? = null
        if (dateInput.isNotEmpty()) {
            try {
                newDate = LocalDate.parse(dateInput)
            } catch (e: DateTimeParseException) {
                println("Invalid date format."); return
            }
        }

        print("New start time (HH:MM, leave blank to keep): ")
        val timeInput = scanner.nextLine().trim()
        var newStartTime: LocalTime? = null
        if (timeInput.isNotEmpty()) {
            try {
                newStartTime = LocalTime.parse(timeInput)
            } catch (e: DateTimeParseException) {
                println("Invalid time format."); return
            }
        }

        print("New duration minutes (leave blank to keep): ")
        val durationInput = scanner.nextLine().trim()
        var newDuration: Int? = null
        if (durationInput.isNotEmpty()) {
            newDuration = durationInput.toIntOrNull()
            if (newDuration == null || newDuration <= 0) {
                println("Duration must be a positive integer."); return
            }
        }

        val result = validator.validateUpdate(id, newDate, newStartTime, newDuration)
        if (!result.valid) {
            println("Validation failed:")
            result.errors.forEach { println("  - $it") }
            return
        }

        print("New description (leave blank to keep): ")
        val newDescription = scanner.nextLine().trim()

        try {
            val updated = service.updateBooking(id, newDate, newStartTime, newDuration, newDescription)
            println("Booking updated: $updated")
        } catch (e: Exception) {
            println("Error: ${e.message}")
        }
    }

    // ── 7. Statistics ──────────────────────────────────────────────

    private fun showStatistics() {
        val stats = service.getStatistics()
        println("--- Booking Statistics ---")
        println("Total:     ${stats["total"]}")
        println("Confirmed: ${stats["confirmed"]}")
        println("Cancelled: ${stats["cancelled"]}")
        println("Capacity:  ${service.capacity}")
        println("Quoted revenue: $%.2f".format(service.totalQuotedRevenue()))
    }

    // ── 8. Export to CSV ───────────────────────────────────────────

    private fun exportToCsv() {
        print("File path (default: bookings.csv): ")
        var path = scanner.nextLine().trim()
        if (path.isEmpty()) path = "bookings.csv"
        try {
            service.exportToCsv(path)
            println("Bookings exported to $path")
        } catch (e: IOException) {
            println("Export failed: ${e.message}")
        }
    }

    // ── 9. Generate report ─────────────────────────────────────────

    private fun generateReport() {
        println("Report type:")
        println("  a) Summary report")
        println("  b) Daily schedule")
        println("  c) Customer report")
        print("Choice: ")
        val type = scanner.nextLine().trim().lowercase()

        val report: String = when (type) {
            "a" -> reportGenerator.generateSummaryReport()
            "b" -> {
                print("From date (YYYY-MM-DD): ")
                val from: LocalDate
                try {
                    from = LocalDate.parse(scanner.nextLine().trim())
                } catch (e: DateTimeParseException) {
                    println("Invalid date format."); return
                }
                print("To date (YYYY-MM-DD): ")
                val to: LocalDate
                try {
                    to = LocalDate.parse(scanner.nextLine().trim())
                } catch (e: DateTimeParseException) {
                    println("Invalid date format."); return
                }
                try {
                    reportGenerator.generateDailySchedule(from, to)
                } catch (e: IllegalArgumentException) {
                    println("Error: ${e.message}"); return
                }
            }
            "c" -> {
                print("Customer name: ")
                val name = scanner.nextLine().trim()
                if (name.isEmpty()) { println("Name cannot be empty."); return }
                reportGenerator.generateCustomerReport(name)
            }
            else -> { println("Invalid report type."); return }
        }

        println("\n$report")

        print("Save to file? (y/n): ")
        if (scanner.nextLine().trim().equals("y", ignoreCase = true)) {
            print("File path: ")
            val path = scanner.nextLine().trim()
            try {
                reportGenerator.saveToFile(report, path)
                println("Report saved to $path")
            } catch (e: IOException) {
                println("Save failed: ${e.message}")
            }
        }
    }

    // ── 10. Advanced search / filter ───────────────────────────────

    private fun advancedSearch() {
        val filter = BookingFilter(service.listBookings())

        print("Filter by status? (CONFIRMED/CANCELLED/blank for all): ")
        val statusInput = scanner.nextLine().trim().uppercase()
        if (statusInput.isNotEmpty()) {
            try {
                filter.byStatus(Booking.Status.valueOf(statusInput))
            } catch (e: IllegalArgumentException) {
                println("Invalid status, showing all.")
            }
        }

        print("From date? (YYYY-MM-DD or blank): ")
        val fromInput = scanner.nextLine().trim()
        if (fromInput.isNotEmpty()) {
            try {
                filter.fromDate(LocalDate.parse(fromInput))
            } catch (e: DateTimeParseException) {
                println("Invalid date, skipping from-date filter.")
            }
        }

        print("To date? (YYYY-MM-DD or blank): ")
        val toInput = scanner.nextLine().trim()
        if (toInput.isNotEmpty()) {
            try {
                filter.toDate(LocalDate.parse(toInput))
            } catch (e: DateTimeParseException) {
                println("Invalid date, skipping to-date filter.")
            }
        }

        print("Customer name contains? (blank for all): ")
        val customerInput = scanner.nextLine().trim()
        if (customerInput.isNotEmpty()) {
            filter.byCustomer(customerInput)
        }

        print("Sort by? (date/customer/status, default date): ")
        val sortInput = scanner.nextLine().trim().lowercase()
        val sortField = when (sortInput) {
            "customer" -> BookingFilter.SortField.CUSTOMER_NAME
            "status"   -> BookingFilter.SortField.STATUS
            else       -> BookingFilter.SortField.DATE
        }

        print("Order? (asc/desc, default asc): ")
        val ascending = !scanner.nextLine().trim().equals("desc", ignoreCase = true)

        filter.sortBy(sortField, ascending)

        print("Limit results? (number or blank for all): ")
        val limitInput = scanner.nextLine().trim()
        if (limitInput.isNotEmpty()) {
            try {
                filter.limit(limitInput.toInt())
            } catch (e: NumberFormatException) {
                println("Invalid number, showing all.")
            }
        }

        println(filter.formatResults())
    }

    // ── 11. View audit log ─────────────────────────────────────────

    private fun viewAuditLog() {
        val auditLog = service.auditLog
        val entries = auditLog.getAll()

        if (entries.isEmpty()) { println("Audit log is empty."); return }

        entries.forEach(::println)
        println("\n${auditLog.summary()}")
    }

    // ── 12. Booking history ────────────────────────────────────────

    private fun viewBookingHistory() {
        print("Booking ID: ")
        val id = scanner.nextLine().trim()

        val history = service.auditLog.getByBookingId(id)
        if (history.isEmpty()) {
            println("No history found for booking $id.")
        } else {
            println("History for booking $id:")
            history.forEach(::println)
        }
    }

    // ── 13. Quote price ────────────────────────────────────────────

    private fun quotePrice() {
        print("Booking ID: ")
        val id = scanner.nextLine().trim()
        print("Customer type (REGULAR/VIP/CORPORATE): ")
        val type = scanner.nextLine().trim()
        print("Party size: ")
        val party = scanner.nextLine().trim().toIntOrNull()
        if (party == null || party <= 0) {
            println("Error: Party size must be a positive integer.")
            return
        }
        print("Loyalty years: ")
        val loyalty = scanner.nextLine().trim().toIntOrNull()
        if (loyalty == null || loyalty < 0) {
            println("Error: Loyalty years must be a non-negative integer.")
            return
        }
        print("Coupon code (blank for none): ")
        val coupon = scanner.nextLine().trim().ifEmpty { null }
        print("Prepay? (y/n): ")
        val prepay = scanner.nextLine().trim().equals("y", ignoreCase = true)
        print("Season (HIGH/LOW/MID): ")
        val season = scanner.nextLine().trim()
        print("Save quote to file? (path or blank): ")
        val saveTo = scanner.nextLine().trim().ifEmpty { null }

        try {
            pricer.calculateAndPrintAndMaybeSave(
                id, type, party, loyalty, coupon, prepay, season, saveTo
            )
        } catch (e: IllegalArgumentException) {
            println("Error: ${e.message}")
        }
    }

    // ── 14. Set capacity ───────────────────────────────────────────

    private fun setCapacity() {
        print("New capacity (positive integer, current ${service.capacity}): ")
        val value = scanner.nextLine().trim().toIntOrNull()
        if (value == null) {
            println("Capacity must be an integer."); return
        }
        try {
            service.capacity = value
            println("Capacity set to ${service.capacity}.")
        } catch (e: IllegalArgumentException) {
            println("Error: ${e.message}")
        }
    }
}
