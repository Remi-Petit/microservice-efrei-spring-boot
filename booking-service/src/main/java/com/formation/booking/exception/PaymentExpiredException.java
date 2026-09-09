package com.formation.booking.exception;

/**
 * Levee lorsque le delai de paiement (bookingDate + 1h) est depasse.
 */
public class PaymentExpiredException extends RuntimeException {

    public PaymentExpiredException(Long bookingId) {
        super("Le delai de paiement de la reservation " + bookingId + " est expire");
    }
}
