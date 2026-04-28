package com.booking.model

import java.time.LocalDateTime

/**
 * Snapshot of a price quote attached to a booking.
 *
 * Captures the inputs used by [com.booking.service.BookingPricer] so the
 * total can be reconstructed or audited later, without re-running pricing.
 */
data class Quote(
    val total: Double,
    val customerType: String,
    val partySize: Int,
    val loyaltyYears: Int,
    val couponCode: String?,
    val prepay: Boolean,
    val season: String,
    val quotedAt: LocalDateTime = LocalDateTime.now()
)
