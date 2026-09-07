package com.formation.loan.exception;

/**
 * Aucun exemplaire disponible pour cet emprunt.
 * Traduit en 409 Conflict : règle métier bloquante (l'etat de la ressource ne
 * permet pas de satisfaire la demande). Le projet guide n'avait pas encore
 * utilise ce code HTTP : 400 = requete mal formee, 409 = conflit d'etat.
 */
public class InsufficientCopiesForLoanException extends RuntimeException {

    public InsufficientCopiesForLoanException(Long bookId) {
        super("Aucun exemplaire disponible pour le livre " + bookId);
    }
}
