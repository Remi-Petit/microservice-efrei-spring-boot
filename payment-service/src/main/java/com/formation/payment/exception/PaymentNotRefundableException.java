package com.formation.payment.exception;

/**
 * Levee lorsque le remboursement n'est pas possible (paiement deja rembourse
 * ou paiement en echec).
 */
public class PaymentNotRefundableException extends RuntimeException {

    public PaymentNotRefundableException(String message) {
        super(message);
    }
}
