package com.formation.book.exception;

public class BookNotFoundException extends RuntimeException {

    public BookNotFoundException(Long id) {
        super("Livre introuvable avec l'identifiant " + id);
    }
}
