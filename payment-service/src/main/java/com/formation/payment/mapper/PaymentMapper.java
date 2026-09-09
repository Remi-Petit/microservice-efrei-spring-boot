package com.formation.payment.mapper;

import com.formation.payment.dto.PaymentRequest;
import com.formation.payment.dto.PaymentResponse;
import com.formation.payment.model.Payment;
import com.formation.payment.model.PaymentStatus;
import com.formation.payment.util.ReferenceGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

public final class PaymentMapper {

    private PaymentMapper() {
    }

    public static PaymentResponse toResponse(Payment p) {
        if (p == null) {
            return null;
        }
        return new PaymentResponse(
                p.getId(),
                p.getPaymentReference(),
                p.getBookingId(),
                p.getBookingReference(),
                p.getUserId(),
                p.getAmount(),
                p.getPaymentMethod(),
                p.getCardLastFour(),
                p.getTransactionId(),
                p.getPaymentDate(),
                p.getStatus()
        );
    }

    /**
     * Construit une entite {@link Payment} a partir d'une requete.
     * Le statut initial est PENDING, la date de paiement est fixee a maintenant.
     */
    public static Payment toEntity(PaymentRequest request) {
        String transactionId = request.transactionId() != null && !request.transactionId().isBlank()
                ? request.transactionId()
                : "txn-" + UUID.randomUUID().toString().substring(0, 6);
        return new Payment(
                ReferenceGenerator.nextPaymentReference(),
                request.bookingId(),
                request.bookingReference(),
                request.userId(),
                request.amount(),
                request.paymentMethod(),
                request.cardLastFour(),
                transactionId,
                LocalDateTime.now(),
                PaymentStatus.PENDING
        );
    }
}
