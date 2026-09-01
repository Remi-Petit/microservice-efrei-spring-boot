package com.formation.order.exception;

/**
 * product-service est indisponible (timeout, connexion refusee, erreur 5xx...).
 * Traduit en 502 Bad Gateway (panne d'infrastructure, pas la faute du client).
 */
public class ProductServiceUnavailableException extends RuntimeException {

    public ProductServiceUnavailableException(Throwable cause) {
        super("product-service est indisponible, impossible de traiter la commande", cause);
    }
}
