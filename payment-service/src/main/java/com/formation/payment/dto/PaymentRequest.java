package com.formation.payment.dto;

import com.formation.payment.model.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Corps de requete pour traiter un paiement (appele par booking-service).
 */
public record PaymentRequest(
        @NotNull(message = "L'identifiant de la reservation est obligatoire")
        Long bookingId,

        @NotBlank(message = "La reference de la reservation est obligatoire")
        String bookingReference,

        @NotNull(message = "L'identifiant de l'utilisateur est obligatoire")
        Long userId,

        @NotNull(message = "Le montant est obligatoire")
        @DecimalMin(value = "0.00", message = "Le montant doit etre positif")
        BigDecimal amount,

        @NotNull(message = "Le moyen de paiement est obligatoire")
        PaymentMethod paymentMethod,

        String cardLastFour,

        String transactionId
) {
}
