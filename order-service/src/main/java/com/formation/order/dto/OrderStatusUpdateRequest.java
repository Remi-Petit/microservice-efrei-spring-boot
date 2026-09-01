package com.formation.order.dto;

import com.formation.order.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(
        @NotNull(message = "Le statut est obligatoire")
        OrderStatus status
) {
}
