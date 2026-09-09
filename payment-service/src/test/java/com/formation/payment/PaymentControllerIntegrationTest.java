package com.formation.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formation.payment.dto.PaymentRequest;
import com.formation.payment.model.PaymentMethod;
import com.formation.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    private PaymentRequest request(BigDecimal amount) {
        return new PaymentRequest(1L, "BK-12345", 7L, amount,
                PaymentMethod.PAYPAL, null, "txn_abc");
    }

    @Test
    void process_petitMontant_accepte() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(new BigDecimal("45.00")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.paymentReference").isNotEmpty());
    }

    @Test
    void process_grosMontant_refuse() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(new BigDecimal("120.00")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void process_requeteInvalide_retourne400() throws Exception {
        PaymentRequest invalid = new PaymentRequest(null, "BK-1", 7L, new BigDecimal("20.00"),
                PaymentMethod.STRIPE, null, null);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getByBooking_recupereLePaiement() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(new BigDecimal("25.00")))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/payments/booking/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingReference").value("BK-12345"));
    }

    @Test
    void getByBooking_aucunPaiement_retourne404() throws Exception {
        mockMvc.perform(get("/api/payments/booking/12345"))
                .andExpect(status().isNotFound());
    }
}
