package com.formation.booking.exception;

/**
 * Levee lorsqu'une transition d'etat d'une reservation n'est pas autorisee
 * (deja annulee, deja terminee, annulation hors delai, cours non ouvert...).
 */
public class InvalidBookingOperationException extends RuntimeException {

    public InvalidBookingOperationException(String message) {
        super(message);
    }
}
