# Booking System

A command-line booking system built with Kotlin. Supports full booking lifecycle management with validation, audit logging, reporting, advanced search, time-slot scheduling with configurable capacity, and persistent price quotes.

## Prerequisites

- Kotlin 1.9+
- JDK 17+

## Build & Run

```bash
kotlinc \
  src/main/kotlin/com/booking/model/Booking.kt \
  src/main/kotlin/com/booking/model/Quote.kt \
  src/main/kotlin/com/booking/service/AuditLog.kt \
  src/main/kotlin/com/booking/service/BookingValidator.kt \
  src/main/kotlin/com/booking/service/BookingService.kt \
  src/main/kotlin/com/booking/service/BookingPricer.kt \
  src/main/kotlin/com/booking/service/ReportGenerator.kt \
  src/main/kotlin/com/booking/util/BookingFilter.kt \
  src/main/kotlin/com/booking/App.kt \
  -include-runtime -d booking.jar

java -jar booking.jar
```

## Features

- **Create booking** — date, start time, duration, with full validation (duplicates, advance notice, weekend rules, capacity)
- **Time slots & capacity** — bookings have a start time and duration; configurable per-system capacity rejects new/rescheduled slots whose overlapping confirmed bookings would exceed it
- **List bookings** — view all bookings with status and (if quoted) total
- **Find booking** — look up a booking by ID
- **Cancel booking** — cancel an existing booking by ID
- **Search by customer** — case-insensitive partial name matching
- **Update/reschedule** — modify date, time, duration, or description with validation
- **Statistics** — total, confirmed, cancelled counts, capacity, and quoted revenue
- **Export to CSV** — save bookings (including times and quote totals) to a CSV file
- **Generate reports** — summary, daily schedule, or per-customer reports (console or file)
- **Advanced search** — filter by status, date range, customer; sort and limit results
- **Audit log** — view full mutation history across all bookings (create, update, cancel, export, quote)
- **Booking history** — view the change trail for a single booking
- **Price quotes** — quote a booking and persist the result on the booking itself, so totals show up in listings, reports, and CSV exports

## Project Structure

```text
src/main/kotlin/com/booking/
├── model/
│   ├── Booking.kt                # Booking entity with status, time slot, attached quote
│   └── Quote.kt                  # Persisted price-quote snapshot
├── service/
│   ├── AuditLog.kt               # Immutable event log for all mutations
│   ├── BookingPricer.kt          # Pricing calculator that persists quotes back to bookings
│   ├── BookingService.kt         # Core CRUD, capacity, and overlap logic
│   ├── BookingValidator.kt       # Composable validation rules (incl. capacity)
│   └── ReportGenerator.kt        # Summary, schedule, and customer reports
├── util/
│   └── BookingFilter.kt          # Fluent sort/filter utility
└── App.kt                        # CLI entry point
```
