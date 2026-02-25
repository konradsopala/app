package com.booking.model

import java.time.LocalDate
import java.util.UUID

/**
 * Booking entity representing a single reservation in the system.
 */
class Booking(
    val customerName: String,
    var date: LocalDate,
    var description: String
) {
    enum class Status {
        CONFIRMED, CANCELLED
    }

    val id: String = UUID.randomUUID().toString().substring(0, 8)
    var status: Status = Status.CONFIRMED
        private set

    fun cancel() {
        status = Status.CANCELLED
    }

    override fun toString(): String =
        "[$id] $customerName | $date | $description | $status"
}
