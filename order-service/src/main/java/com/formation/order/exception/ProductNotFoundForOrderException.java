package com.formation.order.exception;

/**
 * Le produit demande dans la commande est introuvable chez product-service.
 * Traduit en 400 Bad Request (faute du client).
 */
public class ProductNotFoundForOrderException extends RuntimeException {

    public ProductNotFoundForOrderException(Long productId) {
        super("Le produit " + productId + " demande dans la commande est introuvable");
    }
}
