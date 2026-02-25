package com.booking;

import com.booking.model.Booking;
import com.booking.service.AuditLog;
import com.booking.service.BookingService;
import com.booking.service.BookingValidator;
import com.booking.service.BookingValidator.ValidationResult;
import com.booking.service.ReportGenerator;
import com.booking.util.BookingFilter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class App {

    private final BookingService service = new BookingService();
    private final BookingValidator validator = new BookingValidator(service);
    private final ReportGenerator reportGenerator = new ReportGenerator(service);
    private final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        new App().run();
    }

    private void run() {
        System.out.println("=== Booking System ===");

        while (true) {
            System.out.println("\n 1) Create booking");
            System.out.println(" 2) List bookings");
            System.out.println(" 3) Find booking");
            System.out.println(" 4) Cancel booking");
            System.out.println(" 5) Search by customer");
            System.out.println(" 6) Update booking");
            System.out.println(" 7) Statistics");
            System.out.println(" 8) Export to CSV");
            System.out.println(" 9) Generate report");
            System.out.println("10) Advanced search");
            System.out.println("11) View audit log");
            System.out.println("12) Booking history");
            System.out.println("13) Exit");
            System.out.print("\nChoice: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1"  -> createBooking();
                case "2"  -> listBookings();
                case "3"  -> findBooking();
                case "4"  -> cancelBooking();
                case "5"  -> searchByCustomer();
                case "6"  -> updateBooking();
                case "7"  -> showStatistics();
                case "8"  -> exportToCsv();
                case "9"  -> generateReport();
                case "10" -> advancedSearch();
                case "11" -> viewAuditLog();
                case "12" -> viewBookingHistory();
                case "13" -> {
                    System.out.println("Goodbye!");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    // ── 1. Create ──────────────────────────────────────────────────

    private void createBooking() {
        System.out.print("Customer name: ");
        String name = scanner.nextLine().trim();

        System.out.print("Date (YYYY-MM-DD): ");
        LocalDate date;
        try {
            date = LocalDate.parse(scanner.nextLine().trim());
        } catch (DateTimeParseException e) {
            System.out.println("Invalid date format.");
            return;
        }

        System.out.print("Description: ");
        String description = scanner.nextLine().trim();

        ValidationResult result = validator.validateNewBooking(name, date, description);
        if (!result.valid()) {
            System.out.println("Validation failed:");
            result.errors().forEach(err -> System.out.println("  - " + err));
            return;
        }

        try {
            Booking booking = service.createBooking(name, date, description);
            System.out.println("Booking created: " + booking);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // ── 2. List ────────────────────────────────────────────────────

    private void listBookings() {
        List<Booking> bookings = service.listBookings();
        if (bookings.isEmpty()) {
            System.out.println("No bookings found.");
            return;
        }
        bookings.forEach(System.out::println);
    }

    // ── 3. Find ────────────────────────────────────────────────────

    private void findBooking() {
        System.out.print("Booking ID: ");
        String id = scanner.nextLine().trim();
        Booking booking = service.findBooking(id);
        if (booking == null) {
            System.out.println("Booking not found.");
        } else {
            System.out.println(booking);
        }
    }

    // ── 4. Cancel ──────────────────────────────────────────────────

    private void cancelBooking() {
        System.out.print("Booking ID to cancel: ");
        String id = scanner.nextLine().trim();
        if (service.cancelBooking(id)) {
            System.out.println("Booking cancelled.");
        } else {
            System.out.println("Booking not found or already cancelled.");
        }
    }

    // ── 5. Search by customer ──────────────────────────────────────

    private void searchByCustomer() {
        System.out.print("Customer name to search: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("Search term cannot be empty.");
            return;
        }
        List<Booking> results = service.searchByCustomer(name);
        if (results.isEmpty()) {
            System.out.println("No bookings found for \"" + name + "\".");
        } else {
            System.out.println("Found " + results.size() + " booking(s):");
            results.forEach(System.out::println);
        }
    }

    // ── 6. Update / reschedule ─────────────────────────────────────

    private void updateBooking() {
        System.out.print("Booking ID to update: ");
        String id = scanner.nextLine().trim();

        System.out.print("New date (YYYY-MM-DD, leave blank to keep): ");
        String dateInput = scanner.nextLine().trim();
        LocalDate newDate = null;
        if (!dateInput.isEmpty()) {
            try {
                newDate = LocalDate.parse(dateInput);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format.");
                return;
            }
        }

        ValidationResult result = validator.validateUpdate(newDate);
        if (!result.valid()) {
            System.out.println("Validation failed:");
            result.errors().forEach(err -> System.out.println("  - " + err));
            return;
        }

        System.out.print("New description (leave blank to keep): ");
        String newDescription = scanner.nextLine().trim();

        try {
            Booking updated = service.updateBooking(id, newDate, newDescription);
            System.out.println("Booking updated: " + updated);
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // ── 7. Statistics ──────────────────────────────────────────────

    private void showStatistics() {
        Map<String, Long> stats = service.getStatistics();
        System.out.println("--- Booking Statistics ---");
        System.out.println("Total:     " + stats.get("total"));
        System.out.println("Confirmed: " + stats.get("confirmed"));
        System.out.println("Cancelled: " + stats.get("cancelled"));
    }

    // ── 8. Export to CSV ───────────────────────────────────────────

    private void exportToCsv() {
        System.out.print("File path (default: bookings.csv): ");
        String path = scanner.nextLine().trim();
        if (path.isEmpty()) {
            path = "bookings.csv";
        }
        try {
            service.exportToCsv(path);
            System.out.println("Bookings exported to " + path);
        } catch (IOException e) {
            System.out.println("Export failed: " + e.getMessage());
        }
    }

    // ── 9. Generate report ─────────────────────────────────────────

    private void generateReport() {
        System.out.println("Report type:");
        System.out.println("  a) Summary report");
        System.out.println("  b) Daily schedule");
        System.out.println("  c) Customer report");
        System.out.print("Choice: ");
        String type = scanner.nextLine().trim().toLowerCase();

        String report;
        switch (type) {
            case "a" -> report = reportGenerator.generateSummaryReport();
            case "b" -> {
                System.out.print("From date (YYYY-MM-DD): ");
                LocalDate from;
                try {
                    from = LocalDate.parse(scanner.nextLine().trim());
                } catch (DateTimeParseException e) {
                    System.out.println("Invalid date format.");
                    return;
                }
                System.out.print("To date (YYYY-MM-DD): ");
                LocalDate to;
                try {
                    to = LocalDate.parse(scanner.nextLine().trim());
                } catch (DateTimeParseException e) {
                    System.out.println("Invalid date format.");
                    return;
                }
                report = reportGenerator.generateDailySchedule(from, to);
            }
            case "c" -> {
                System.out.print("Customer name: ");
                String name = scanner.nextLine().trim();
                if (name.isEmpty()) {
                    System.out.println("Name cannot be empty.");
                    return;
                }
                report = reportGenerator.generateCustomerReport(name);
            }
            default -> {
                System.out.println("Invalid report type.");
                return;
            }
        }

        System.out.println("\n" + report);

        System.out.print("Save to file? (y/n): ");
        if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
            System.out.print("File path: ");
            String path = scanner.nextLine().trim();
            try {
                reportGenerator.saveToFile(report, path);
                System.out.println("Report saved to " + path);
            } catch (IOException e) {
                System.out.println("Save failed: " + e.getMessage());
            }
        }
    }

    // ── 10. Advanced search / filter ───────────────────────────────

    private void advancedSearch() {
        BookingFilter filter = new BookingFilter(service.listBookings());

        System.out.print("Filter by status? (CONFIRMED/CANCELLED/blank for all): ");
        String statusInput = scanner.nextLine().trim().toUpperCase();
        if (!statusInput.isEmpty()) {
            try {
                filter.byStatus(Booking.Status.valueOf(statusInput));
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid status, showing all.");
            }
        }

        System.out.print("From date? (YYYY-MM-DD or blank): ");
        String fromInput = scanner.nextLine().trim();
        if (!fromInput.isEmpty()) {
            try {
                filter.fromDate(LocalDate.parse(fromInput));
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date, skipping from-date filter.");
            }
        }

        System.out.print("To date? (YYYY-MM-DD or blank): ");
        String toInput = scanner.nextLine().trim();
        if (!toInput.isEmpty()) {
            try {
                filter.toDate(LocalDate.parse(toInput));
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date, skipping to-date filter.");
            }
        }

        System.out.print("Customer name contains? (blank for all): ");
        String customerInput = scanner.nextLine().trim();
        if (!customerInput.isEmpty()) {
            filter.byCustomer(customerInput);
        }

        System.out.print("Sort by? (date/customer/status, default date): ");
        String sortInput = scanner.nextLine().trim().toLowerCase();
        BookingFilter.SortField sortField = switch (sortInput) {
            case "customer" -> BookingFilter.SortField.CUSTOMER_NAME;
            case "status"   -> BookingFilter.SortField.STATUS;
            default         -> BookingFilter.SortField.DATE;
        };

        System.out.print("Order? (asc/desc, default asc): ");
        boolean ascending = !scanner.nextLine().trim().equalsIgnoreCase("desc");

        filter.sortBy(sortField, ascending);

        System.out.print("Limit results? (number or blank for all): ");
        String limitInput = scanner.nextLine().trim();
        if (!limitInput.isEmpty()) {
            try {
                filter.limit(Integer.parseInt(limitInput));
            } catch (NumberFormatException e) {
                System.out.println("Invalid number, showing all.");
            }
        }

        System.out.println(filter.formatResults());
    }

    // ── 11. View audit log ─────────────────────────────────────────

    private void viewAuditLog() {
        AuditLog auditLog = service.getAuditLog();
        List<AuditLog.Entry> entries = auditLog.getAll();

        if (entries.isEmpty()) {
            System.out.println("Audit log is empty.");
            return;
        }

        entries.forEach(System.out::println);
        System.out.println("\n" + auditLog.summary());
    }

    // ── 12. Booking history ────────────────────────────────────────

    private void viewBookingHistory() {
        System.out.print("Booking ID: ");
        String id = scanner.nextLine().trim();

        List<AuditLog.Entry> history = service.getAuditLog().getByBookingId(id);
        if (history.isEmpty()) {
            System.out.println("No history found for booking " + id + ".");
        } else {
            System.out.println("History for booking " + id + ":");
            history.forEach(System.out::println);
        }
    }
}
