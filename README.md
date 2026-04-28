# Booking System

A command-line booking system built with Kotlin. Supports full booking lifecycle management with validation, audit logging, reporting, advanced search, time-slot scheduling with configurable capacity, persistent price quotes, file-backed persistence, CSV import, and authenticated multi-user access with roles.

## Prerequisites

- Kotlin 1.9+
- JDK 17+

## Build & Run

```bash
# Build the app
find src/main -name "*.kt" -print0 | xargs -0 kotlinc -include-runtime -d booking.jar

# Run it
java -jar booking.jar
```

On first launch the app seeds a default admin (`admin` / `admin123`) — change the password immediately via the *Manage users* menu. State is persisted to `./data/` after every change and reloaded on startup.

## Run tests

```bash
# Compile main + tests into a single jar
find src -name "*.kt" -print0 | xargs -0 kotlinc -include-runtime -d booking-tests.jar

# Run the test suite (exits non-zero on any failure)
java -cp booking-tests.jar com.booking.test.TestRunnerKt
```

## Features

- **Authentication** — login required at startup, with `ADMIN` and `USER` roles. Passwords are hashed with PBKDF2-WithHmacSHA256 (JDK built-in). Admin-only menu items: view audit log, set capacity, manage users.
- **Persistence** — bookings, audit log, users, and the capacity setting are saved to `./data/*.tsv` after every mutation and reloaded on startup. State survives restarts.
- **CSV import** — header-driven importer accepting `customer,date,start,duration,description`; each row passes through validation and capacity checks; failed rows are reported with reasons.
- **Create booking** — date, start time, duration, with full validation (duplicates, advance notice, weekend rules, capacity)
- **Time slots & capacity** — bookings have a start time and duration; a configurable per-system capacity rejects new/rescheduled slots whose overlapping confirmed bookings would exceed it
- **List bookings** — view all bookings with status and (if quoted) total
- **Find / cancel / update / search** — standard CRUD with case-insensitive partial customer-name matching and reschedule validation
- **Statistics** — total, confirmed, cancelled counts, capacity, and quoted revenue
- **Export to CSV** — save bookings (including times and quote totals) to a CSV file
- **Generate reports** — summary, daily schedule, or per-customer reports (console or file)
- **Advanced search** — filter by status, date range, customer; sort and limit results
- **Audit log** — full mutation history including the actor (authenticated username) for every action
- **Booking history** — view the change trail for a single booking
- **Price quotes** — quote a booking and persist the result on the booking itself, so totals show up in listings, reports, and CSV exports

## Project Structure

```text
src/main/kotlin/com/booking/
├── App.kt                        # CLI entry point with login + menu
├── model/
│   ├── Booking.kt                # Booking entity with status, time slot, attached quote
│   ├── Quote.kt                  # Persisted price-quote snapshot
│   └── User.kt                   # Authenticated user with role
├── service/
│   ├── AuditLog.kt               # Immutable event log (with actor) for all mutations
│   ├── BookingPricer.kt          # Pricing calculator that persists quotes back to bookings
│   ├── BookingService.kt         # Core CRUD, capacity, overlap, audit/onChange hooks
│   ├── BookingValidator.kt       # Composable validation rules (incl. capacity)
│   ├── CsvImporter.kt            # Header-driven CSV import with per-row validation
│   ├── PersistenceService.kt     # File-backed save/load for bookings, audit, users, settings
│   ├── ReportGenerator.kt        # Summary, schedule, and customer reports
│   └── UserService.kt            # Auth: PBKDF2 hashing, login/logout, user management
└── util/
    ├── BookingFilter.kt          # Fluent sort/filter utility
    └── PasswordHasher.kt         # PBKDF2-WithHmacSHA256 password hashing

src/test/kotlin/com/booking/test/
├── TestRunner.kt                 # In-process test harness + assertion DSL + main()
├── BookingPricerTests.kt
├── BookingServiceTests.kt
├── BookingValidatorTests.kt
├── CsvImporterTests.kt
├── PersistenceServiceTests.kt
└── UserServiceTests.kt
```

## Persisted state

The `./data/` directory is created on first save and contains:

- `bookings.tsv` — booking records with attached quotes
- `audit.tsv` — audit log
- `users.tsv` — usernames, password hashes, roles
- `settings.tsv` — capacity and other tunables

Files use TSV with `\` escapes for tab/newline/backslash so arbitrary text fields round-trip cleanly. No external dependencies.
