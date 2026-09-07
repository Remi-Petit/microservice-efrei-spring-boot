package com.formation.book.exception;

/**
 * Tentative de creer / modifier un livre avec un ISBN deja utilise.
 * Traduit en 409 Conflict : la ressource viole une contrainte d'unicite.
 */
public class DuplicateIsbnException extends RuntimeException {

    public DuplicateIsbnException(String isbn) {
        super("Un livre avec l'ISBN " + isbn + " existe deja");
    }
}
