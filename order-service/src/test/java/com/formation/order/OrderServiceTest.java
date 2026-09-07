package com.formation.order;

import com.formation.order.client.ProductClient;
import com.formation.order.dto.OrderItemRequest;
import com.formation.order.dto.OrderRequest;
import com.formation.order.dto.OrderResponse;
import com.formation.order.dto.ProductDto;
import com.formation.order.dto.OrderStatusUpdateRequest;
import com.formation.order.exception.OrderNotFoundException;
import com.formation.order.exception.ProductNotFoundForOrderException;
import com.formation.order.exception.ProductServiceUnavailableException;
import com.formation.order.model.Order;
import com.formation.order.model.OrderStatus;
import com.formation.order.repository.OrderRepository;
import com.formation.order.service.OrderService;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private OrderService orderService;

    private ProductDto product(Long id, String name, String price) {
        ProductDto dto = new ProductDto();
        dto.setId(id);
        dto.setName(name);
        dto.setPrice(new BigDecimal(price));
        dto.setQuantity(10);
        return dto;
    }

    private FeignException.NotFound notFound() {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "/api/products/999",
                new HashMap<>(),
                (byte[]) null,
                null
        );
        return new FeignException.NotFound("Not Found", request, null, new HashMap<>());
    }

    private FeignException feignError() {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "/api/products/1",
                new HashMap<>(),
                (byte[]) null,
                null
        );
        return new FeignException.ServiceUnavailable("Service Unavailable", request, null, new HashMap<>());
    }

    @Test
    void create_calculeLeTotalEtSauvegarde() {
        when(productClient.getProductById(eq(1L))).thenReturn(product(1L, "Clavier mecanique", "79.90"));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            return new Order(o.getCustomerName(), o.getCreatedAt(), o.getStatus(), new BigDecimal("159.80"));
        });

        OrderRequest request = new OrderRequest("Alice", List.of(new OrderItemRequest(1L, 2)));

        OrderResponse result = orderService.create(request);

        assertThat(result.totalAmount()).isEqualByComparingTo("159.80");
    }

    @Test
    void create_produitInexistant_leveProductNotFoundForOrderException() {
        when(productClient.getProductById(eq(999L))).thenThrow(notFound());

        OrderRequest request = new OrderRequest("Alice", List.of(new OrderItemRequest(999L, 1)));

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(ProductNotFoundForOrderException.class);
    }

    @Test
    void create_serviceIndisponible_leveProductServiceUnavailableException() {
        when(productClient.getProductById(eq(1L))).thenThrow(feignError());

        OrderRequest request = new OrderRequest("Alice", List.of(new OrderItemRequest(1L, 1)));

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(ProductServiceUnavailableException.class);
    }

    @Test
    void findById_commandeInexistante_leveOrderNotFoundException() {
        when(orderRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(7L))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void updateStatus_changeLeStatut() {
        Order order = new Order("Alice", java.time.Instant.now(), OrderStatus.CREATED, BigDecimal.ZERO);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse result = orderService.updateStatus(1L, new OrderStatusUpdateRequest(OrderStatus.CONFIRMED));

        assertThat(result.status()).isEqualTo(OrderStatus.CONFIRMED);
    }
}
