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

        @NotNull(message = "Le nombre d'exemplaires est obligatoire")
        @Min(value = 0, message = "Le nombre d'exemplaires ne peut pas etre negatif")
        Integer availableCopies
) {
}
