package com.formation.loan.exception;

/**
 * Le livre demande dans l'emprunt est introuvable chez book-service.
 * Traduit en 400 Bad Request (faute du client).
 */
public class BookNotFoundForLoanException extends RuntimeException {

    public BookNotFoundForLoanException(Long bookId) {
        super("Le livre " + bookId + " demande dans l'emprunt est introuvable");
    }
}
