package com.booking.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

/**
 * A pending booking request held until capacity frees up at the requested slot.
 *
 * Entries are FIFO-ordered by [addedAt]; promotion is the responsibility of
 * [com.booking.service.WaitlistService.tryPromoteAll].
 */
data class WaitlistEntry(
    val customerName: String,
    val date: LocalDate,
    val startTime: LocalTime,
    val durationMinutes: Int,
    val description: String,
    val id: String = UUID.randomUUID().toString().substring(0, 8),
    val addedAt: LocalDateTime = LocalDateTime.now()
) {
    val endTime: LocalTime
        get() = startTime.plusMinutes(durationMinutes.toLong())

    override fun toString(): String =
        "[wl:$id] $customerName | $date $startTime-$endTime | $description (queued $addedAt)"
}
