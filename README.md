# Booking System

A command-line booking system built with Kotlin. Supports full booking lifecycle management with validation, audit logging, reporting, advanced search, time-slot scheduling with configurable capacity, persistent price quotes, recurring booking series, and a capacity-aware FIFO waitlist.

## Prerequisites

- Kotlin 1.9+
- JDK 17+

## Build & Run

```bash
find src -name "*.kt" -print0 | xargs -0 kotlinc -include-runtime -d booking.jar
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
- **Audit log** — view full mutation history across all bookings (create, update, cancel, export, quote, waitlist, promote, series cancel)
- **Booking history** — view the change trail for a single booking
- **Price quotes** — quote a booking and persist the result on the booking itself, so totals show up in listings, reports, and CSV exports
- **Recurring series** — create N occurrences on a daily/weekly/biweekly/monthly cadence; each occurrence is independently validated, collisions are skipped and reported. Every booking in the series carries a shared `seriesId` so the whole series can be cancelled in one go.
- **Waitlist** — when a new booking is rejected solely on capacity, the CLI offers to add it to a FIFO waitlist. After every cancellation (or capacity bump) the system walks the queue and promotes any entry whose slot now passes full validation, in order. Waitlist entries can be listed and removed manually.

## Project Structure

```text
src/main/kotlin/com/booking/
├── model/
│   ├── Booking.kt                # Booking entity (status, time slot, attached quote, optional seriesId)
│   ├── Quote.kt                  # Persisted price-quote snapshot
│   └── WaitlistEntry.kt          # Pending booking request held until capacity frees up
├── service/
│   ├── AuditLog.kt               # Immutable event log for all mutations
│   ├── BookingPricer.kt          # Pricing calculator that persists quotes back to bookings
│   ├── BookingService.kt         # Core CRUD, capacity, overlap, series queries
│   ├── BookingValidator.kt       # Composable validation rules (incl. capacity)
│   ├── RecurringBookingService.kt# Series creation (DAILY/WEEKLY/BIWEEKLY/MONTHLY) and bulk cancel
│   ├── ReportGenerator.kt        # Summary, schedule, and customer reports
│   └── WaitlistService.kt        # FIFO queue with capacity-aware promotion
├── util/
│   └── BookingFilter.kt          # Fluent sort/filter utility
└── App.kt                        # CLI entry point
```
