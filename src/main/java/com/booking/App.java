package com.booking;

import com.booking.model.Booking;
import com.booking.service.BookingService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
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
            System.out.println("5) Exit");
            System.out.print("\nChoice: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> createBooking();
                case "2" -> listBookings();
                case "3" -> findBooking();
                case "4" -> cancelBooking();
                case "5" -> {
                    System.out.println("Goodbye!");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

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

        Booking booking = service.createBooking(name, date, description);
        System.out.println("Booking created: " + booking);
    }

    private void listBookings() {
        List<Booking> bookings = service.listBookings();
        if (bookings.isEmpty()) {
            System.out.println("No bookings found.");
            return;
        }
        bookings.forEach(System.out::println);
    }

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

    private void cancelBooking() {
        System.out.print("Booking ID to cancel: ");
        String id = scanner.nextLine().trim();
        if (service.cancelBooking(id)) {
            System.out.println("Booking cancelled.");
        } else {
            System.out.println("Booking not found or already cancelled.");
        }
    }
}
