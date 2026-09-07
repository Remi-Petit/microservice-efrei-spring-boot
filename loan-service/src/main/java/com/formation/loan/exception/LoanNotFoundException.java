package com.formation.loan.exception;

public class LoanNotFoundException extends RuntimeException {

    public LoanNotFoundException(Long id) {
        super("Emprunt introuvable avec l'identifiant " + id);
    }
}
