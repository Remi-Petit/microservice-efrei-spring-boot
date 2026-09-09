package com.formation.booking.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Corps de requete pour confirmer une reservation apres paiement.
 * NB : {@code paymentMethod} est une chaine (ex. "CREDIT_CARD") qui sera
 * mappee sur l'enum correspondant cote payment-service.
 */
public record ConfirmPaymentRequest(
        @NotBlank(message = "Le moyen de paiement est obligatoire")
        String paymentMethod,

        String cardLastFour,

        String transactionId
) {
}
