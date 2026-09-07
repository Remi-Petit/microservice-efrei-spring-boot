package com.formation.book.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookRequest(
        @NotBlank(message = "L'ISBN est obligatoire")
        String isbn,

        @NotBlank(message = "Le titre est obligatoire")
        String title,

        @NotBlank(message = "L'auteur est obligatoire")
        String author,

        @NotNull(message = "Le nombre total d'exemplaires est obligatoire")
        @Min(value = 1, message = "Le nombre total d'exemplaires doit etre au moins 1")
        Integer totalCopies
) {
}

