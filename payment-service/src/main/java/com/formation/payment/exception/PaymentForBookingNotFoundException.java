package com.formation.payment.exception;

/**
 * Levee lorsqu'aucun paiement n'existe pour une reservation donnee.
 */
public class PaymentForBookingNotFoundException extends RuntimeException {

    public PaymentForBookingNotFoundException(Long bookingId) {
        super("Aucun paiement pour la reservation " + bookingId);
    }
}
