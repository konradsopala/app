package com.booking.service

import com.booking.model.Booking
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException

/**
 * Imports bookings from a CSV file.
 *
 * The header row identifies columns by name; required columns are
 * `customer`, `date`, `start`, `duration`, `description`. Extra columns
 * (e.g. `id`, `status`, `quote_total` from an export) are ignored — IDs are
 * always regenerated and bookings always start CONFIRMED.
 *
 * Each row is run through [BookingValidator]; rows that fail validation are
 * collected and reported. Rows are imported in order so capacity checks
 * apply against bookings imported earlier in the same file.
 */
class CsvImporter(
    private val bookingService: BookingService,
    private val validator: BookingValidator
) {

    data class Row(val lineNumber: Int, val customer: String, val date: LocalDate, val start: LocalTime, val duration: Int, val description: String)
    data class Failure(val lineNumber: Int, val raw: String, val errors: List<String>)
    data class Result(val imported: List<Booking>, val failures: List<Failure>)

    fun import(file: File): Result {
        require(file.isFile) { "Not a regular file: ${file.absolutePath}" }
        val imported = mutableListOf<Booking>()
        val failures = mutableListOf<Failure>()

        file.useLines { lines ->
            val iterator = lines.iterator()
            if (!iterator.hasNext()) return Result(imported, failures)

            val header = parseRow(iterator.next()).map { it.trim().lowercase() }
            val idx = mapOf(
                "customer" to header.indexOf("customer"),
                "date" to header.indexOf("date"),
                "start" to header.indexOf("start"),
                "duration" to header.indexOf("duration"),
                "description" to header.indexOf("description")
            )
            val missing = idx.filterValues { it < 0 }.keys
            require(missing.isEmpty()) { "CSV is missing required columns: $missing" }

            var lineNo = 1
            while (iterator.hasNext()) {
                lineNo++
                val raw = iterator.next()
                if (raw.isBlank()) continue
                val cols = parseRow(raw)
                val parseErrors = mutableListOf<String>()

                val customer = cols.getOrNull(idx["customer"]!!)?.trim().orEmpty()
                val description = cols.getOrNull(idx["description"]!!)?.trim().orEmpty()

                val date: LocalDate? = try {
                    cols.getOrNull(idx["date"]!!)?.trim()?.let(LocalDate::parse)
                } catch (e: DateTimeParseException) {
                    parseErrors.add("Invalid date: ${e.message}"); null
                }

                val start: LocalTime? = try {
                    cols.getOrNull(idx["start"]!!)?.trim()?.let(LocalTime::parse)
                } catch (e: DateTimeParseException) {
                    parseErrors.add("Invalid start time: ${e.message}"); null
                }

                val duration: Int? = cols.getOrNull(idx["duration"]!!)?.trim()?.toIntOrNull().also {
                    if (it == null) parseErrors.add("Invalid duration (must be an integer).")
                }

                if (parseErrors.isNotEmpty() || date == null || start == null || duration == null) {
                    failures.add(Failure(lineNo, raw, parseErrors.ifEmpty { listOf("Missing required field.") }))
                    continue
                }

                val validation = validator.validateNewBooking(customer, date, start, duration, description)
                if (!validation.valid) {
                    failures.add(Failure(lineNo, raw, validation.errors))
                    continue
                }

                try {
                    val booking = bookingService.createBooking(customer, date, start, duration, description)
                    imported.add(booking)
                } catch (e: IllegalArgumentException) {
                    failures.add(Failure(lineNo, raw, listOf(e.message ?: "Unknown error.")))
                }
            }
        }

        if (imported.isNotEmpty()) {
            bookingService.auditLog.log(
                "SYSTEM",
                AuditLog.Action.IMPORTED,
                "Imported ${imported.size} booking(s) from ${file.name}",
                bookingService.currentActor
            )
        }
        return Result(imported, failures)
    }

    /** Minimal RFC-4180 row parser: supports quoted fields with embedded commas and `""` escapes. */
    private fun parseRow(line: String): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    cur.append('"'); i += 2
                }
                c == '"' -> { inQuotes = !inQuotes; i++ }
                !inQuotes && c == ',' -> { out.add(cur.toString()); cur.clear(); i++ }
                else -> { cur.append(c); i++ }
            }
        }
        out.add(cur.toString())
        return out
    }
}
