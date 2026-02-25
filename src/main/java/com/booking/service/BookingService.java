package com.booking.service;

import com.booking.model.Booking;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BookingService {

    private final Map<String, Booking> bookings = new LinkedHashMap<>();
    private final AuditLog auditLog = new AuditLog();

    public AuditLog getAuditLog() {
        return auditLog;
    }

    // ── Create ──────────────────────────────────────────────────────

    public Booking createBooking(String customerName, LocalDate date, String description) {
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Booking date cannot be in the past.");
        }
        Booking booking = new Booking(customerName, date, description);
        bookings.put(booking.getId(), booking);
        auditLog.log(booking.getId(), AuditLog.Action.CREATED,
                "Customer: " + customerName + ", Date: " + date);
        return booking;
    }

    // ── Cancel ──────────────────────────────────────────────────────

    public boolean cancelBooking(String id) {
        Booking booking = bookings.get(id);
        if (booking == null || booking.getStatus() == Booking.Status.CANCELLED) {
            return false;
        }
        booking.cancel();
        auditLog.log(id, AuditLog.Action.CANCELLED, "Cancelled by user");
        return true;
    }

    // ── Find ────────────────────────────────────────────────────────

    public Booking findBooking(String id) {
        return bookings.get(id);
    }

    // ── List ────────────────────────────────────────────────────────

    public List<Booking> listBookings() {
        return new ArrayList<>(bookings.values());
    }

    // ── Search by customer name (case-insensitive, partial match) ──

    public List<Booking> searchByCustomer(String name) {
        String lowerName = name.toLowerCase();
        return bookings.values().stream()
                .filter(b -> b.getCustomerName().toLowerCase().contains(lowerName))
                .collect(Collectors.toList());
    }

    // ── Update / reschedule ─────────────────────────────────────────

    public Booking updateBooking(String id, LocalDate newDate, String newDescription) {
        Booking booking = bookings.get(id);
        if (booking == null) {
            throw new IllegalArgumentException("Booking not found.");
        }
        if (booking.getStatus() == Booking.Status.CANCELLED) {
            throw new IllegalStateException("Cannot update a cancelled booking.");
        }
        if (newDate != null) {
            if (newDate.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("New date cannot be in the past.");
            }
            booking.setDate(newDate);
        }
        if (newDescription != null && !newDescription.isBlank()) {
            booking.setDescription(newDescription);
        }
        auditLog.log(id, AuditLog.Action.UPDATED,
                "Date: " + booking.getDate() + ", Desc: " + booking.getDescription());
        return booking;
    }

    // ── Statistics ──────────────────────────────────────────────────

    public Map<String, Long> getStatistics() {
        long total = bookings.size();
        long confirmed = bookings.values().stream()
                .filter(b -> b.getStatus() == Booking.Status.CONFIRMED).count();
        long cancelled = bookings.values().stream()
                .filter(b -> b.getStatus() == Booking.Status.CANCELLED).count();

        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("confirmed", confirmed);
        stats.put("cancelled", cancelled);
        return stats;
    }

    // ── Export to CSV ───────────────────────────────────────────────

    public void exportToCsv(String filePath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("id,customer,date,description,status");
            for (Booking b : bookings.values()) {
                writer.printf("%s,%s,%s,%s,%s%n",
                        escape(b.getId()),
                        escape(b.getCustomerName()),
                        b.getDate(),
                        escape(b.getDescription()),
                        b.getStatus());
            }
        }
        auditLog.log("SYSTEM", AuditLog.Action.EXPORTED, "Exported to " + filePath);
    }

    private String escape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
