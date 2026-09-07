package com.formation.book.exception;

/**
 * Tentative de re-incrementer le stock au-dela du nombre total d'exemplaires.
 * Traduit en 409 Conflict : l'etat du livre ne permet pas cette operation.
 */
public class TooManyCopiesException extends RuntimeException {

    public TooManyCopiesException(Long bookId) {
        super("Le stock du livre " + bookId + " ne peut pas depasser le nombre total d'exemplaires");
    }
}
