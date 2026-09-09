package com.formation.payment.dto;

import com.formation.payment.model.PaymentMethod;
import com.formation.payment.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        String paymentReference,
        Long bookingId,
        String bookingReference,
        Long userId,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        String cardLastFour,
        String transactionId,
        LocalDateTime paymentDate,
        PaymentStatus status
) {
}
