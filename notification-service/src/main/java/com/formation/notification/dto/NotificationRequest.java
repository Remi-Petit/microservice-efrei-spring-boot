package com.formation.notification.dto;

import com.formation.notification.model.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Corps de requete pour envoyer une notification (appele par les autres services).
 */
public record NotificationRequest(
        @NotNull(message = "L'identifiant de l'utilisateur est obligatoire")
        Long userId,

        String email,

        @NotNull(message = "Le type de notification est obligatoire")
        NotificationType type,

        @NotBlank(message = "Le sujet est obligatoire")
        String subject,

        @NotBlank(message = "Le contenu est obligatoire")
        String content
) {
}
