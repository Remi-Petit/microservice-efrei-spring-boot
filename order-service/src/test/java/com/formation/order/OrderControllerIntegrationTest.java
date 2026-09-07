package com.formation.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formation.order.client.ProductClient;
import com.formation.order.dto.OrderItemRequest;
import com.formation.order.dto.OrderRequest;
import com.formation.order.dto.OrderStatusUpdateRequest;
import com.formation.order.dto.ProductDto;
import com.formation.order.model.Order;
import com.formation.order.model.OrderStatus;
import com.formation.order.repository.OrderRepository;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @MockBean
    private ProductClient productClient;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
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

    @Test
    void cycleDeVieComplet_creerListerRecupererChangerStatutSupprimer() throws Exception {
        ProductDto product = new ProductDto();
        product.setId(1L);
        product.setName("Clavier mecanique");
        product.setPrice(new BigDecimal("79.90"));
        when(productClient.getProductById(eq(1L))).thenReturn(product);

        OrderRequest createRequest = new OrderRequest("Alice", List.of(new OrderItemRequest(1L, 2)));

        String response = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(159.80))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productName").value("Clavier mecanique"));

        mockMvc.perform(patch("/api/orders/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderStatusUpdateRequest(OrderStatus.CONFIRMED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void create_produitInexistantChezProductService_retourne400() throws Exception {
        when(productClient.getProductById(eq(999L))).thenThrow(notFound());

        OrderRequest createRequest = new OrderRequest("Alice", List.of(new OrderItemRequest(999L, 1)));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listeToutesLesCommandes_retourneLaListe() throws Exception {
        orderRepository.save(new Order("Alice", java.time.Instant.now(), OrderStatus.CREATED, new BigDecimal("159.80")));
        orderRepository.save(new Order("Bob", java.time.Instant.now(), OrderStatus.CONFIRMED, new BigDecimal("79.90")));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].customerName").value("Alice"))
                .andExpect(jsonPath("$[1].customerName").value("Bob"));
    }

    @Test
    void supprimer_commandeExistante_retourne204() throws Exception {
        Order saved = orderRepository.save(new Order("Alice", java.time.Instant.now(), OrderStatus.CREATED, new BigDecimal("79.90")));

        mockMvc.perform(delete("/api/orders/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/orders/{id}", saved.getId()))
                .andExpect(status().isNotFound());
    }
}
