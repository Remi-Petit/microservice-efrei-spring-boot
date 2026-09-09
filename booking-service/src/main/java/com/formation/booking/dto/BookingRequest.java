package com.formation.booking.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Corps de requete pour creer une reservation.
 */
public record BookingRequest(
        @NotNull(message = "L'identifiant de l'utilisateur est obligatoire")
        Long userId,

        @NotBlank(message = "L'email de l'utilisateur est obligatoire")
        String userEmail,

        @NotBlank(message = "Le nom de l'utilisateur est obligatoire")
        String userName,

        @NotNull(message = "L'identifiant du cours est obligatoire")
        Long classId,

        @NotNull(message = "Le nombre de places est obligatoire")
        @Min(value = 1, message = "Au moins 1 place doit etre reservee")
        @Max(value = 4, message = "Maximum 4 places par reservation")
        Integer numberOfSpots
) {
}
