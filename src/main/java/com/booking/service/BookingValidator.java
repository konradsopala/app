package com.booking.service;

import com.booking.model.Booking;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BookingValidator {

    public record ValidationResult(boolean valid, List<String> errors) {
        public static ValidationResult ok() {
            return new ValidationResult(true, List.of());
        }

        public static ValidationResult fail(List<String> errors) {
            return new ValidationResult(false, List.copyOf(errors));
        }
    }

    private final BookingService service;
    private int maxBookingsPerCustomer = 10;
    private int minAdvanceDays = 1;
    private boolean blockWeekends = false;

    public BookingValidator(BookingService service) {
        this.service = service;
    }

    // ── Configuration ────────────────────────────────────────────

    public void setMaxBookingsPerCustomer(int max) {
        this.maxBookingsPerCustomer = max;
    }

    public void setMinAdvanceDays(int days) {
        this.minAdvanceDays = days;
    }

    public void setBlockWeekends(boolean block) {
        this.blockWeekends = block;
    }

    // ── Validate new booking ─────────────────────────────────────

    public ValidationResult validateNewBooking(String customerName, LocalDate date, String description) {
        List<String> errors = new ArrayList<>();

        if (customerName == null || customerName.isBlank()) {
            errors.add("Customer name cannot be empty.");
        }

        if (date != null && date.isBefore(LocalDate.now())) {
            errors.add("Booking date cannot be in the past.");
        }

        if (date != null && date.isBefore(LocalDate.now().plusDays(minAdvanceDays))) {
            errors.add("Booking must be at least " + minAdvanceDays + " day(s) in advance.");
        }

        if (blockWeekends && date != null) {
            DayOfWeek dow = date.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                errors.add("Weekend bookings are not allowed.");
            }
        }

        if (customerName != null && date != null) {
            boolean duplicate = service.listBookings().stream()
                    .filter(b -> b.getStatus() == Booking.Status.CONFIRMED)
                    .anyMatch(b -> b.getCustomerName().equalsIgnoreCase(customerName)
                            && b.getDate().equals(date));
            if (duplicate) {
                errors.add("Customer already has a booking on " + date + ".");
            }
        }

        if (customerName != null) {
            long count = service.listBookings().stream()
                    .filter(b -> b.getStatus() == Booking.Status.CONFIRMED)
                    .filter(b -> b.getCustomerName().equalsIgnoreCase(customerName))
                    .count();
            if (count >= maxBookingsPerCustomer) {
                errors.add("Customer has reached the maximum of " + maxBookingsPerCustomer + " bookings.");
            }
        }

        if (description == null || description.isBlank()) {
            errors.add("Description cannot be empty.");
        }

        return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.fail(errors);
    }

    // ── Validate update ──────────────────────────────────────────

    public ValidationResult validateUpdate(LocalDate newDate) {
        List<String> errors = new ArrayList<>();

        if (newDate != null && newDate.isBefore(LocalDate.now())) {
            errors.add("New date cannot be in the past.");
        }

        if (newDate != null && newDate.isBefore(LocalDate.now().plusDays(minAdvanceDays))) {
            errors.add("Rescheduled date must be at least " + minAdvanceDays + " day(s) in advance.");
        }

        if (blockWeekends && newDate != null) {
            DayOfWeek dow = newDate.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                errors.add("Cannot reschedule to a weekend.");
            }
        }

        return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.fail(errors);
    }
}
