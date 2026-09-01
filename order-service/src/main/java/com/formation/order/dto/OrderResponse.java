package com.formation.order.dto;

import com.formation.order.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        String customerName,
        Instant createdAt,
        OrderStatus status,
        BigDecimal totalAmount,
        List<OrderItemResponse> items
) {
}
