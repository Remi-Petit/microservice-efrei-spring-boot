package com.formation.booking.exception;

/**
 * Levee lorsqu'un service distant appele en interne est indisponible.
 */
public class BookingServiceUnavailableException extends RuntimeException {

    public BookingServiceUnavailableException(String message) {
        super(message);
    }
}
