package com.booking.model;

import java.time.LocalDate;
import java.util.UUID;

public class Booking {

    public enum Status {
        CONFIRMED, CANCELLED
    }

    private final String id;
    private final String customerName;
    private final LocalDate date;
    private final String description;
    private Status status;

    public Booking(String customerName, LocalDate date, String description) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.customerName = customerName;
        this.date = date;
        this.description = description;
        this.status = Status.CONFIRMED;
    }

    public String getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public Status getStatus() {
        return status;
    }

    public void cancel() {
        this.status = Status.CANCELLED;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | %s | %s | %s", id, customerName, date, description, status);
    }
}
