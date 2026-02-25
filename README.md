# Booking System

A simple command-line booking system built with Java. Supports creating, listing, finding, and cancelling bookings with in-memory storage.

## Prerequisites

- Java 17+

## Build & Run

```bash
javac -d out src/main/java/com/booking/model/Booking.java src/main/java/com/booking/service/BookingService.java src/main/java/com/booking/App.java
java -cp out com.booking.App
```

## Features

- **Create booking** — provide customer name, date, and description
- **List bookings** — view all bookings with their status
- **Find booking** — look up a booking by ID
- **Cancel booking** — cancel an existing booking by ID

## Project Structure

```
src/main/java/com/booking/
├── model/
│   └── Booking.java          # Booking entity
├── service/
│   └── BookingService.java   # CRUD operations
└── App.java                  # CLI entry point
```
