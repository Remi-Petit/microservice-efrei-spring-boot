package com.formation.book.exception;

/**
 * Le livre n'a plus d'exemplaire disponible pour un emprunt.
 * Traduit en 409 Conflict : l'etat de la ressource ne permet pas de satisfaire
 * la demande (règle métier bloquante), par opposition a 400 qui signale une
 * requete mal formee.
 */
public class InsufficientCopiesException extends RuntimeException {

    public InsufficientCopiesException(Long bookId) {
        super("Le livre " + bookId + " n'a plus d'exemplaire disponible");
    }
}
