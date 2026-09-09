package com.formation.classsvc.dto;

import com.formation.classsvc.model.Category;
import com.formation.classsvc.model.Level;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Corps de requete pour la creation / la mise a jour d'un cours.
 * NB : {@code durationMinutes} doit valoir 30, 45, 60 ou 90 (verifie dans le service).
 */
public record FitnessClassRequest(
        @NotBlank(message = "Le nom du cours est obligatoire")
        @Size(min = 3, message = "Le nom du cours doit contenir au moins 3 caracteres")
        String name,

        @NotBlank(message = "La description est obligatoire")
        String description,

        @NotBlank(message = "L'instructeur est obligatoire")
        String instructor,

        @NotBlank(message = "La localisation (salle) est obligatoire")
        String gymLocation,

        @NotNull(message = "La categorie est obligatoire")
        Category category,

        @NotNull(message = "Le niveau est obligatoire")
        Level level,

        @NotNull(message = "La duree est obligatoire")
        Integer durationMinutes,

        @NotNull(message = "Le nombre maximum de participants est obligatoire")
        @Min(value = 5, message = "Le nombre maximum de participants doit etre au moins 5")
        @Max(value = 30, message = "Le nombre maximum de participants ne peut pas depasser 30")
        Integer maxParticipants,

        @NotNull(message = "Le prix est obligatoire")
        @DecimalMin(value = "5.00", message = "Le prix doit etre au moins 5.00")
        BigDecimal price,

        @NotNull(message = "La date et l'heure du cours sont obligatoires")
        @Future(message = "La date du cours doit etre dans le futur")
        LocalDateTime dateTime
) {
}
