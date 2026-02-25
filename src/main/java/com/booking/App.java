package com.booking;

import com.booking.model.Booking;
import com.booking.service.BookingService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class App {

    private final BookingService service = new BookingService();
    private final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        new App().run();
    }

    private void run() {
        System.out.println("=== Booking System ===");

        while (true) {
            System.out.println("\n1) Create booking");
            System.out.println("2) List bookings");
            System.out.println("3) Find booking");
            System.out.println("4) Cancel booking");
            System.out.println("5) Search by customer");
            System.out.println("6) Update booking");
            System.out.println("7) Statistics");
            System.out.println("8) Export to CSV");
            System.out.println("9) Exit");
            System.out.print("\nChoice: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> createBooking();
                case "2" -> listBookings();
                case "3" -> findBooking();
                case "4" -> cancelBooking();
                case "5" -> searchByCustomer();
                case "6" -> updateBooking();
                case "7" -> showStatistics();
                case "8" -> exportToCsv();
                case "9" -> {
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
        if (name.isEmpty()) {
            System.out.println("Name cannot be empty.");
            return;
        }

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
}
