package com.booking.service;

import com.booking.model.Booking;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BookingService {

    private final Map<String, Booking> bookings = new LinkedHashMap<>();

    public Booking createBooking(String customerName, LocalDate date, String description) {
        Booking booking = new Booking(customerName, date, description);
        bookings.put(booking.getId(), booking);
        return booking;
    }

    public boolean cancelBooking(String id) {
        Booking booking = bookings.get(id);
        if (booking == null || booking.getStatus() == Booking.Status.CANCELLED) {
            return false;
        }
        booking.cancel();
        return true;
    }

    public Booking findBooking(String id) {
        return bookings.get(id);
    }

    public List<Booking> listBookings() {
        return new ArrayList<>(bookings.values());
    }
}
