package com.booking.service;

import com.booking.model.Booking;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportGenerator {

    private final BookingService service;

    public ReportGenerator(BookingService service) {
        this.service = service;
    }

    // ── Summary Report ───────────────────────────────────────────

    public String generateSummaryReport() {
        List<Booking> all = service.listBookings();
        Map<String, Long> stats = service.getStatistics();
        StringBuilder sb = new StringBuilder();

        sb.append("===================================\n");
        sb.append("       BOOKING SUMMARY REPORT      \n");
        sb.append("===================================\n");
        sb.append("Generated: ").append(LocalDate.now()).append("\n\n");

        sb.append("-- Overview --\n");
        sb.append(String.format("  Total bookings:     %d%n", stats.get("total")));
        sb.append(String.format("  Confirmed:          %d%n", stats.get("confirmed")));
        sb.append(String.format("  Cancelled:          %d%n", stats.get("cancelled")));

        sb.append("\n-- Top Customers --\n");
        all.stream()
                .filter(b -> b.getStatus() == Booking.Status.CONFIRMED)
                .collect(Collectors.groupingBy(Booking::getCustomerName, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> sb.append(String.format("  %-20s %d booking(s)%n", e.getKey(), e.getValue())));

        sb.append("\n-- Busiest Dates --\n");
        all.stream()
                .filter(b -> b.getStatus() == Booking.Status.CONFIRMED)
                .collect(Collectors.groupingBy(Booking::getDate, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<LocalDate, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> sb.append(String.format("  %s    %d booking(s)%n", e.getKey(), e.getValue())));

        sb.append("\n-- Upcoming (next 7 days) --\n");
        LocalDate today = LocalDate.now();
        LocalDate weekOut = today.plusDays(7);
        List<Booking> upcoming = all.stream()
                .filter(b -> b.getStatus() == Booking.Status.CONFIRMED)
                .filter(b -> !b.getDate().isBefore(today) && b.getDate().isBefore(weekOut))
                .sorted(Comparator.comparing(Booking::getDate))
                .collect(Collectors.toList());
        if (upcoming.isEmpty()) {
            sb.append("  (none)\n");
        } else {
            upcoming.forEach(b -> sb.append("  ").append(b).append("\n"));
        }

        return sb.toString();
    }

    // ── Daily Schedule Report ────────────────────────────────────

    public String generateDailySchedule(LocalDate from, LocalDate to) {
        StringBuilder sb = new StringBuilder();

        sb.append("===================================\n");
        sb.append("       DAILY SCHEDULE REPORT       \n");
        sb.append(String.format("       %s to %s%n", from, to));
        sb.append("===================================\n\n");

        Map<LocalDate, List<Booking>> byDate = service.listBookings().stream()
                .filter(b -> b.getStatus() == Booking.Status.CONFIRMED)
                .filter(b -> !b.getDate().isBefore(from) && !b.getDate().isAfter(to))
                .sorted(Comparator.comparing(Booking::getDate))
                .collect(Collectors.groupingBy(Booking::getDate));

        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            sb.append("-- ").append(cursor).append(" (").append(cursor.getDayOfWeek()).append(") --\n");
            List<Booking> dayBookings = byDate.getOrDefault(cursor, List.of());
            if (dayBookings.isEmpty()) {
                sb.append("  (no bookings)\n");
            } else {
                dayBookings.forEach(b -> sb.append("  ").append(b).append("\n"));
            }
            sb.append("\n");
            cursor = cursor.plusDays(1);
        }

        return sb.toString();
    }

    // ── Customer Report ──────────────────────────────────────────

    public String generateCustomerReport(String customerName) {
        List<Booking> customerBookings = service.searchByCustomer(customerName);
        StringBuilder sb = new StringBuilder();

        sb.append("===================================\n");
        sb.append("         CUSTOMER REPORT           \n");
        sb.append(String.format("         Customer: %s%n", customerName));
        sb.append("===================================\n\n");

        if (customerBookings.isEmpty()) {
            sb.append("No bookings found for this customer.\n");
            return sb.toString();
        }

        long confirmed = customerBookings.stream()
                .filter(b -> b.getStatus() == Booking.Status.CONFIRMED).count();
        long cancelled = customerBookings.stream()
                .filter(b -> b.getStatus() == Booking.Status.CANCELLED).count();

        sb.append(String.format("Total: %d  |  Confirmed: %d  |  Cancelled: %d%n%n",
                customerBookings.size(), confirmed, cancelled));

        customerBookings.stream()
                .sorted(Comparator.comparing(Booking::getDate))
                .forEach(b -> sb.append("  ").append(b).append("\n"));

        return sb.toString();
    }

    // ── Save report to file ──────────────────────────────────────

    public void saveToFile(String report, String filePath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.print(report);
        }
    }
}
