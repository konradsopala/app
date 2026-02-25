package com.booking.util;

import com.booking.model.Booking;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BookingFilter {

    public enum SortField {
        DATE, CUSTOMER_NAME, STATUS
    }

    private final List<Booking> source;
    private Booking.Status statusFilter;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String customerPattern;
    private SortField sortField = SortField.DATE;
    private boolean ascending = true;
    private int limit = 0;

    public BookingFilter(List<Booking> bookings) {
        this.source = bookings;
    }

    // ── Fluent filter setters ────────────────────────────────────

    public BookingFilter byStatus(Booking.Status status) {
        this.statusFilter = status;
        return this;
    }

    public BookingFilter fromDate(LocalDate from) {
        this.fromDate = from;
        return this;
    }

    public BookingFilter toDate(LocalDate to) {
        this.toDate = to;
        return this;
    }

    public BookingFilter byCustomer(String pattern) {
        this.customerPattern = pattern;
        return this;
    }

    public BookingFilter sortBy(SortField field, boolean ascending) {
        this.sortField = field;
        this.ascending = ascending;
        return this;
    }

    public BookingFilter limit(int max) {
        this.limit = max;
        return this;
    }

    // ── Execute: apply all filters and sort ──────────────────────

    public List<Booking> apply() {
        Stream<Booking> stream = source.stream();

        if (statusFilter != null) {
            stream = stream.filter(b -> b.getStatus() == statusFilter);
        }
        if (fromDate != null) {
            stream = stream.filter(b -> !b.getDate().isBefore(fromDate));
        }
        if (toDate != null) {
            stream = stream.filter(b -> !b.getDate().isAfter(toDate));
        }
        if (customerPattern != null && !customerPattern.isBlank()) {
            String lower = customerPattern.toLowerCase();
            stream = stream.filter(b -> b.getCustomerName().toLowerCase().contains(lower));
        }

        Comparator<Booking> comparator = switch (sortField) {
            case DATE -> Comparator.comparing(Booking::getDate);
            case CUSTOMER_NAME -> Comparator.comparing(Booking::getCustomerName, String.CASE_INSENSITIVE_ORDER);
            case STATUS -> Comparator.comparing(b -> b.getStatus().name());
        };
        if (!ascending) {
            comparator = comparator.reversed();
        }
        stream = stream.sorted(comparator);

        if (limit > 0) {
            stream = stream.limit(limit);
        }

        return stream.collect(Collectors.toList());
    }

    // ── Convenience: count results ───────────────────────────────

    public long count() {
        return apply().size();
    }

    // ── Convenience: formatted numbered list ─────────────────────

    public String formatResults() {
        List<Booking> results = apply();
        if (results.isEmpty()) {
            return "No bookings match the criteria.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Found %d booking(s):%n", results.size()));
        int i = 1;
        for (Booking b : results) {
            sb.append(String.format("  %d. %s%n", i++, b));
        }
        return sb.toString();
    }
}
