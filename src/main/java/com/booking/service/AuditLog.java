package com.booking.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AuditLog {

    public enum Action {
        CREATED, CANCELLED, UPDATED, EXPORTED
    }

    public record Entry(LocalDateTime timestamp, String bookingId, Action action, String detail) {
        @Override
        public String toString() {
            return String.format("[%s] %s — %s: %s", timestamp, bookingId, action, detail);
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    // ── Record an event ──────────────────────────────────────────

    public void log(String bookingId, Action action, String detail) {
        entries.add(new Entry(LocalDateTime.now(), bookingId, action, detail));
    }

    // ── Query: all entries ───────────────────────────────────────

    public List<Entry> getAll() {
        return List.copyOf(entries);
    }

    // ── Query: entries for a specific booking ────────────────────

    public List<Entry> getByBookingId(String bookingId) {
        return entries.stream()
                .filter(e -> e.bookingId().equals(bookingId))
                .collect(Collectors.toList());
    }

    // ── Query: entries for a specific action type ────────────────

    public List<Entry> getByAction(Action action) {
        return entries.stream()
                .filter(e -> e.action() == action)
                .collect(Collectors.toList());
    }

    // ── Summary count by action type ─────────────────────────────

    public String summary() {
        var counts = entries.stream()
                .collect(Collectors.groupingBy(Entry::action, Collectors.counting()));
        StringBuilder sb = new StringBuilder("Audit summary: ");
        counts.forEach((action, count) ->
                sb.append(count).append(" ").append(action).append(", "));
        return sb.length() > 16 ? sb.substring(0, sb.length() - 2) : "Audit summary: (empty)";
    }
}
