package com.formation.loan.exception;

/**
 * Tentative de retour sur un emprunt deja rendu.
 * Traduit en 409 Conflict : l'etat de l'emprunt ne permet pas cette operation.
 */
public class LoanAlreadyReturnedException extends RuntimeException {

    public LoanAlreadyReturnedException(Long id) {
        super("L'emprunt " + id + " a deja ete rendu");
    }
}
