package com.formation.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "Le nom est obligatoire")
        String name,

        @NotBlank(message = "La description est obligatoire")
        String description,

        @NotNull(message = "Le prix est obligatoire")
        @DecimalMin(value = "0.0", inclusive = false, message = "Le prix doit etre positif")
        BigDecimal price,

        @NotNull(message = "La quantite est obligatoire")
        @Min(value = 0, message = "La quantite ne peut pas etre negative")
        Integer quantity
) {
}
