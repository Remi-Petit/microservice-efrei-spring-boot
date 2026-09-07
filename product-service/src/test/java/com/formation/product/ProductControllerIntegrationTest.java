package com.formation.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formation.product.dto.ProductRequest;
import com.formation.product.model.Product;
import com.formation.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void cleanDatabase() {
        productRepository.deleteAll();
    }

    @Test
    void cycleDeVieComplet_creerListerRecupererModifierSupprimer() throws Exception {
        ProductRequest createRequest = new ProductRequest("Webcam", "Webcam HD 1080p", new BigDecimal("39.90"), 15);

        String response = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Webcam"))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Webcam"));

        mockMvc.perform(delete("/api/products/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_requeteInvalide_retourne400() throws Exception {
        ProductRequest invalid = new ProductRequest("", "", new BigDecimal("-1"), -1);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listeTousLesProduits_retourneLaListe() throws Exception {
        productRepository.save(new Product("Souris", "Souris optique", new BigDecimal("19.90"), 25));
        productRepository.save(new Product("Clavier", "Clavier mecanique", new BigDecimal("79.90"), 10));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Souris"))
                .andExpect(jsonPath("$[1].name").value("Clavier"));
    }

    @Test
    void modifier_produitExistant_retourneProduitMisAJour() throws Exception {
        Product saved = productRepository.save(new Product("Souris", "Souris optique", new BigDecimal("19.90"), 25));

        ProductRequest updateRequest = new ProductRequest("Souris Pro", "Souris optique sans fil", new BigDecimal("24.90"), 30);

        mockMvc.perform(put("/api/products/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.name").value("Souris Pro"))
                .andExpect(jsonPath("$.description").value("Souris optique sans fil"))
                .andExpect(jsonPath("$.price").value(24.90))
                .andExpect(jsonPath("$.quantity").value(30));
    }
}
