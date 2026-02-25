# Booking System

A command-line booking system built with Kotlin. Supports full booking lifecycle management with validation, audit logging, reporting, and advanced search.

## Prerequisites

- Kotlin 1.9+
- JDK 17+

## Build & Run

```bash
kotlinc \
  src/main/kotlin/com/booking/model/Booking.kt \
  src/main/kotlin/com/booking/service/AuditLog.kt \
  src/main/kotlin/com/booking/service/BookingValidator.kt \
  src/main/kotlin/com/booking/service/BookingService.kt \
  src/main/kotlin/com/booking/service/ReportGenerator.kt \
  src/main/kotlin/com/booking/util/BookingFilter.kt \
  src/main/kotlin/com/booking/App.kt \
  -include-runtime -d booking.jar

java -jar booking.jar
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

```text
src/main/kotlin/com/booking/
├── model/
│   └── Booking.kt                # Booking entity with status enum
├── service/
│   ├── AuditLog.kt               # Immutable event log for all mutations
│   ├── BookingService.kt         # Core CRUD and business logic
│   ├── BookingValidator.kt       # Composable validation rules engine
│   └── ReportGenerator.kt        # Summary, schedule, and customer reports
├── util/
│   └── BookingFilter.kt          # Fluent sort/filter utility
└── App.kt                        # CLI entry point
```
