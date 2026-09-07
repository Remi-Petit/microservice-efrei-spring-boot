package com.formation.loan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoanRequest(
        @NotNull(message = "L'identifiant du livre est obligatoire")
        Long bookId,

        @NotBlank(message = "Le nom de l'emprunteur est obligatoire")
        String borrowerName
) {
}
