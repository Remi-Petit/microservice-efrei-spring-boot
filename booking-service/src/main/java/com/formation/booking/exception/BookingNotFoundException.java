package com.formation.booking.exception;

public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(Long id) {
        super("Reservation introuvable : id " + id);
    }
}
