package com.booking.model

import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * Booking entity representing a single reservation in the system.
 */
class Booking(
    val customerName: String,
    var date: LocalDate,
    var startTime: LocalTime,
    var durationMinutes: Int,
    var description: String,
    val seriesId: String? = null
) {
    enum class Status {
        CONFIRMED, CANCELLED
    }

    val id: String = UUID.randomUUID().toString().substring(0, 8)
    var status: Status = Status.CONFIRMED
        private set

    var quote: Quote? = null
        internal set

    val endTime: LocalTime
        get() = startTime.plusMinutes(durationMinutes.toLong())

    fun cancel() {
        status = Status.CANCELLED
    }

    /** True if this booking's time window on [other.date] overlaps [other]. */
    fun overlaps(other: Booking): Boolean {
        if (date != other.date) return false
        return startTime < other.endTime && other.startTime < endTime
    }

    override fun toString(): String {
        val priceSuffix = quote?.let { " | $%.2f".format(it.total) } ?: ""
        val seriesSuffix = seriesId?.let { " | series:$it" } ?: ""
        return "[$id] $customerName | $date $startTime-$endTime | $description | $status$priceSuffix$seriesSuffix"
    }
}
