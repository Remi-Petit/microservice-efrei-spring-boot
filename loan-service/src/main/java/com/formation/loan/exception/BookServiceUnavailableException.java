package com.formation.loan.exception;

/**
 * book-service est indisponible (timeout, connexion refusee, erreur 5xx...).
 * Traduit en 502 Bad Gateway (panne d'infrastructure, pas la faute du client).
 */
public class BookServiceUnavailableException extends RuntimeException {

    public BookServiceUnavailableException(Throwable cause) {
        super("book-service est indisponible, impossible de traiter l'emprunt", cause);
    }
}
