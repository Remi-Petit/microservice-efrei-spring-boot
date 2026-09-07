package com.formation.loan.exception;

/**
 * Un membre ne peut pas avoir plus de 3 emprunts ACTIVE simultanement (bonus).
 * Traduit en 409 Conflict : règle métier bloquante.
 */
public class ActiveLoanLimitExceededException extends RuntimeException {

    public ActiveLoanLimitExceededException(String memberName) {
        super("Le membre " + memberName + " a deja atteint la limite de 3 emprunts actifs");
    }
}
