package com.booking.service

import com.booking.model.Booking
import com.booking.model.Quote
import com.booking.model.User
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * File-backed persistence for bookings, audit log, and users.
 *
 * Each entity type is stored in its own line-oriented file under [dataDir],
 * with `\` used to escape `\t`, `\n`, and `\\` so arbitrary text fields
 * round-trip cleanly. The format intentionally avoids external dependencies.
 *
 * - `bookings.tsv` — header `id\tcustomer\tdate\tstart\tduration\tdescription\tstatus\tquote_json`
 *   where `quote_json` is empty or a tab-escaped serialisation of the [Quote].
 * - `audit.tsv` — header `timestamp\tbookingId\taction\tactor\tdetail`.
 * - `users.tsv` — header `username\tpasswordHash\trole`.
 */
class PersistenceService(
    private val dataDir: File,
    private val bookingService: BookingService,
    private val userService: UserService
) {

    private val bookingsFile = File(dataDir, "bookings.tsv")
    private val auditFile = File(dataDir, "audit.tsv")
    private val usersFile = File(dataDir, "users.tsv")
    private val settingsFile = File(dataDir, "settings.tsv")

    // ── Save ─────────────────────────────────────────────────────────

    fun save() {
        if (!dataDir.exists() && !dataDir.mkdirs()) {
            throw IllegalStateException("Failed to create data directory: ${dataDir.absolutePath}")
        }
        saveBookings()
        saveAudit()
        saveUsers()
        saveSettings()
    }

    private fun saveBookings() {
        bookingsFile.bufferedWriter().use { w ->
            w.write("id\tcustomer\tdate\tstart\tduration\tdescription\tstatus\tquote")
            w.newLine()
            for (b in bookingService.listBookings()) {
                w.write(
                    listOf(
                        esc(b.id),
                        esc(b.customerName),
                        b.date.toString(),
                        b.startTime.toString(),
                        b.durationMinutes.toString(),
                        esc(b.description),
                        b.status.name,
                        b.quote?.let(::encodeQuote) ?: ""
                    ).joinToString("\t")
                )
                w.newLine()
            }
        }
    }

    private fun saveAudit() {
        auditFile.bufferedWriter().use { w ->
            w.write("timestamp\tbookingId\taction\tactor\tdetail")
            w.newLine()
            for (e in bookingService.auditLog.getAll()) {
                w.write(
                    listOf(
                        e.timestamp.toString(),
                        esc(e.bookingId),
                        e.action.name,
                        esc(e.actor ?: ""),
                        esc(e.detail)
                    ).joinToString("\t")
                )
                w.newLine()
            }
        }
    }

    private fun saveUsers() {
        usersFile.bufferedWriter().use { w ->
            w.write("username\tpasswordHash\trole")
            w.newLine()
            for (u in userService.listUsers()) {
                w.write(listOf(esc(u.username), esc(u.passwordHash), u.role.name).joinToString("\t"))
                w.newLine()
            }
        }
    }

    private fun saveSettings() {
        settingsFile.bufferedWriter().use { w ->
            w.write("key\tvalue")
            w.newLine()
            w.write("capacity\t${bookingService.capacity}")
            w.newLine()
        }
    }

    // ── Load ─────────────────────────────────────────────────────────

    fun load() {
        if (!dataDir.exists()) return
        // Suppress onChange so loading state from disk doesn't trigger a save back.
        val savedHook = bookingService.onChange
        bookingService.onChange = null
        try {
            loadBookings()
            loadAudit()
            loadUsers()
            loadSettings()
        } finally {
            bookingService.onChange = savedHook
        }
    }

    private fun loadBookings() {
        if (!bookingsFile.exists()) return
        bookingService.clearAllForRestore()
        bookingsFile.useLines { lines ->
            lines.drop(1).forEach { line ->
                if (line.isBlank()) return@forEach
                val cols = splitTsv(line)
                if (cols.size < 8) return@forEach
                val id = cols[0]
                val customer = cols[1]
                val date = LocalDate.parse(cols[2])
                val start = LocalTime.parse(cols[3])
                val duration = cols[4].toInt()
                val description = cols[5]
                val status = Booking.Status.valueOf(cols[6])
                val quote = cols[7].takeIf { it.isNotEmpty() }?.let(::decodeQuote)

                val booking = Booking(customer, date, start, duration, description, initialId = id)
                bookingService.restoreBooking(booking, isCancelled = status == Booking.Status.CANCELLED, quote = quote)
            }
        }
    }

    private fun loadAudit() {
        if (!auditFile.exists()) return
        val entries = mutableListOf<AuditLog.Entry>()
        auditFile.useLines { lines ->
            lines.drop(1).forEach { line ->
                if (line.isBlank()) return@forEach
                val cols = splitTsv(line)
                if (cols.size < 5) return@forEach
                entries.add(
                    AuditLog.Entry(
                        timestamp = LocalDateTime.parse(cols[0]),
                        bookingId = cols[1],
                        action = AuditLog.Action.valueOf(cols[2]),
                        actor = cols[3].ifEmpty { null },
                        detail = cols[4]
                    )
                )
            }
        }
        bookingService.auditLog.restoreEntries(entries)
    }

    private fun loadUsers() {
        if (!usersFile.exists()) return
        val users = mutableListOf<User>()
        usersFile.useLines { lines ->
            lines.drop(1).forEach { line ->
                if (line.isBlank()) return@forEach
                val cols = splitTsv(line)
                if (cols.size < 3) return@forEach
                users.add(User(cols[0], cols[1], User.Role.valueOf(cols[2])))
            }
        }
        userService.restoreUsers(users)
    }

    private fun loadSettings() {
        if (!settingsFile.exists()) return
        settingsFile.useLines { lines ->
            lines.drop(1).forEach { line ->
                if (line.isBlank()) return@forEach
                val cols = splitTsv(line)
                if (cols.size < 2) return@forEach
                if (cols[0] == "capacity") {
                    cols[1].toIntOrNull()?.let { bookingService.capacity = it }
                }
            }
        }
    }

    // ── Encoding helpers ────────────────────────────────────────────

    private fun encodeQuote(q: Quote): String =
        listOf(
            "%.2f".format(q.total),
            esc(q.customerType),
            q.partySize.toString(),
            q.loyaltyYears.toString(),
            esc(q.couponCode ?: ""),
            q.prepay.toString(),
            esc(q.season),
            q.quotedAt.toString()
        ).joinToString("|")

    private fun decodeQuote(encoded: String): Quote? {
        val parts = encoded.split("|")
        if (parts.size < 8) return null
        return Quote(
            total = parts[0].toDouble(),
            customerType = parts[1],
            partySize = parts[2].toInt(),
            loyaltyYears = parts[3].toInt(),
            couponCode = parts[4].ifEmpty { null },
            prepay = parts[5].toBoolean(),
            season = parts[6],
            quotedAt = LocalDateTime.parse(parts[7])
        )
    }

    // Escape \, \t, \n so they survive TSV round-tripping.
    private fun esc(s: String): String =
        s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n")

    private fun splitTsv(line: String): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\\' && i + 1 < line.length) {
                when (line[i + 1]) {
                    '\\' -> cur.append('\\')
                    't' -> cur.append('\t')
                    'n' -> cur.append('\n')
                    else -> cur.append(line[i + 1])
                }
                i += 2
            } else if (c == '\t') {
                out.add(cur.toString())
                cur.clear()
                i++
            } else {
                cur.append(c)
                i++
            }
        }
        out.add(cur.toString())
        return out
    }
}
