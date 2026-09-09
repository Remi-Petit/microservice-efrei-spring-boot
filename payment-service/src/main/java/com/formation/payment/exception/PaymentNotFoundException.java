package com.formation.payment.exception;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(Long id) {
        super("Paiement introuvable : id " + id);
    }
}
