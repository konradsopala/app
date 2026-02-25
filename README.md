# Booking System

A command-line booking system built with Java. Supports full booking lifecycle management with validation, audit logging, reporting, and advanced search.

## Prerequisites

- Java 17+

## Build & Run

```bash
javac -d out \
  src/main/java/com/booking/model/Booking.java \
  src/main/java/com/booking/service/AuditLog.java \
  src/main/java/com/booking/service/BookingValidator.java \
  src/main/java/com/booking/service/BookingService.java \
  src/main/java/com/booking/service/ReportGenerator.java \
  src/main/java/com/booking/util/BookingFilter.java \
  src/main/java/com/booking/App.java

java -cp out com.booking.App
```

## Features

- **Create booking** — with full validation (duplicate detection, advance notice, weekend rules)
- **List bookings** — view all bookings with their status
- **Find booking** — look up a booking by ID
- **Cancel booking** — cancel an existing booking by ID
- **Search by customer** — case-insensitive partial name matching
- **Update/reschedule** — modify date and description with validation
- **Statistics** — total, confirmed, and cancelled counts
- **Export to CSV** — save bookings to a CSV file
- **Generate reports** — summary, daily schedule, or per-customer reports (console or file)
- **Advanced search** — filter by status, date range, customer; sort and limit results
- **Audit log** — view full mutation history across all bookings
- **Booking history** — view the change trail for a single booking

## Project Structure

```
src/main/java/com/booking/
├── model/
│   └── Booking.java              # Booking entity with status enum
├── service/
│   ├── AuditLog.java             # Immutable event log for all mutations
│   ├── BookingService.java       # Core CRUD and business logic
│   ├── BookingValidator.java     # Composable validation rules engine
│   └── ReportGenerator.java      # Summary, schedule, and customer reports
├── util/
│   └── BookingFilter.java        # Fluent sort/filter utility
└── App.java                      # CLI entry point
```
