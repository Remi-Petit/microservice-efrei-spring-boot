package com.formation.booking.dto;

import java.math.BigDecimal;

/**
 * Corps envoye a payment-service lors de la confirmation ({@code POST /api/payments}).
 */
public record PaymentRequestDto(
        Long bookingId,
        String bookingReference,
        Long userId,
        BigDecimal amount,
        String paymentMethod,
        String cardLastFour,
        String transactionId
) {
}
