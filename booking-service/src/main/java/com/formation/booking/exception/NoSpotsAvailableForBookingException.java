package com.formation.booking.exception;

/**
 * Levee lorsqu'il n'y a plus assez de places disponibles dans le cours
 * (verifiee lors de la reservation, et re-verifiee cote class-service).
 */
public class NoSpotsAvailableForBookingException extends RuntimeException {

    public NoSpotsAvailableForBookingException(Long classId) {
        super("Plus de places disponibles pour ce cours (id " + classId + ")");
    }
}
