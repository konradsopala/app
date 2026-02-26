package com.booking.service

import com.booking.model.Booking
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Composable validation rules engine for booking operations.
 *
 * Centralizes all business rules (duplicate detection, advance notice, max per customer,
 * weekend blocking) and reports all violations at once via [ValidationResult].
 */
class BookingValidator(private val service: BookingService) {

    data class ValidationResult(val valid: Boolean, val errors: List<String>) {
        companion object {
            fun ok(): ValidationResult = ValidationResult(true, emptyList())
            fun fail(errors: List<String>): ValidationResult = ValidationResult(false, errors.toList())
        }
    }

    var maxBookingsPerCustomer: Int = 10
    var minAdvanceDays: Int = 1
    var blockWeekends: Boolean = false

    // ── Validate new booking ─────────────────────────────────────

    fun validateNewBooking(customerName: String?, date: LocalDate?, description: String?): ValidationResult {
        val errors = mutableListOf<String>()

        if (customerName.isNullOrBlank()) {
            errors.add("Customer name cannot be empty.")
        }

        if (date != null && date.isBefore(LocalDate.now())) {
            errors.add("Booking date cannot be in the past.")
        } else if (date != null && date.isBefore(LocalDate.now().plusDays(minAdvanceDays.toLong()))) {
            errors.add("Booking must be at least $minAdvanceDays day(s) in advance.")
        }

        if (blockWeekends && date != null) {
            val dow = date.dayOfWeek
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                errors.add("Weekend bookings are not allowed.")
            }
        }

        // Cache the list once to avoid repeated collection copying
        if (customerName != null || date != null) {
            val bookings = service.listBookings()

            if (customerName != null && date != null) {
                val duplicate = bookings
                    .filter { it.status == Booking.Status.CONFIRMED }
                    .any { it.customerName.equals(customerName, ignoreCase = true) && it.date == date }
                if (duplicate) {
                    errors.add("Customer already has a booking on $date.")
                }
            }

            if (customerName != null) {
                val count = bookings
                    .filter { it.status == Booking.Status.CONFIRMED }
                    .count { it.customerName.equals(customerName, ignoreCase = true) }
                if (count >= maxBookingsPerCustomer) {
                    errors.add("Customer has reached the maximum of $maxBookingsPerCustomer bookings.")
                }
            }
        }

        if (description.isNullOrBlank()) {
            errors.add("Description cannot be empty.")
        }

        return if (errors.isEmpty()) ValidationResult.ok() else ValidationResult.fail(errors)
    }

    // ── Validate update ──────────────────────────────────────────

    fun validateUpdate(newDate: LocalDate?): ValidationResult {
        val errors = mutableListOf<String>()

        if (newDate != null && newDate.isBefore(LocalDate.now())) {
            errors.add("New date cannot be in the past.")
        } else if (newDate != null && newDate.isBefore(LocalDate.now().plusDays(minAdvanceDays.toLong()))) {
            errors.add("Rescheduled date must be at least $minAdvanceDays day(s) in advance.")
        }

        if (blockWeekends && newDate != null) {
            val dow = newDate.dayOfWeek
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                errors.add("Cannot reschedule to a weekend.")
            }
        }

        return if (errors.isEmpty()) ValidationResult.ok() else ValidationResult.fail(errors)
    }
}
