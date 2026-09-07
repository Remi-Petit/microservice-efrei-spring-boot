package com.formation.book.dto;

public record BookResponse(
        Long id,
        String isbn,
        String title,
        String author,
        Integer totalCopies,
        Integer availableCopies
) {
}
