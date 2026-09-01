package com.formation.product.mapper;

import com.formation.product.dto.ProductRequest;
import com.formation.product.dto.ProductResponse;
import com.formation.product.model.Product;

/**
 * Mappe l'entite vers le DTO et inversement. On ne renvoie jamais l'entite
 * directement au client afin de decoupler le contrat de l'API du modele de
 * persistance.
 */
public final class ProductMapper {

    private ProductMapper() {
    }

    public static ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getQuantity()
        );
    }

    public static Product toEntity(ProductRequest request) {
        return new Product(
                request.name(),
                request.description(),
                request.price(),
                request.quantity()
        );
    }
}
