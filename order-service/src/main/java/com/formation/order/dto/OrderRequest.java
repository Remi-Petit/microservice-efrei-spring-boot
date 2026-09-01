package com.formation.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OrderRequest(
        @NotBlank(message = "Le nom du client est obligatoire")
        String customerName,

        @NotEmpty(message = "Au moins un article est requis")
        List<@Valid OrderItemRequest> items
) {
}
