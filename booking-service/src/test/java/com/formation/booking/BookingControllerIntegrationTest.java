package com.formation.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formation.booking.client.ClassClient;
import com.formation.booking.client.NotificationClient;
import com.formation.booking.client.PaymentClient;
import com.formation.booking.dto.BookingRequest;
import com.formation.booking.dto.ConfirmPaymentRequest;
import com.formation.booking.dto.FitnessClassDto;
import com.formation.booking.dto.PaymentDto;
import com.formation.booking.repository.BookingRepository;
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
import java.time.LocalDateTime;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookingRepository repository;

    @MockBean
    private ClassClient classClient;
    @MockBean
    private PaymentClient paymentClient;
    @MockBean
    private NotificationClient notificationClient;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    private FitnessClassDto cls(int max, int current) {
        FitnessClassDto d = new FitnessClassDto();
        d.setId(1L);
        d.setName("Yoga du matin");
        d.setInstructor("Marie");
        d.setPrice(new BigDecimal("20.00"));
        d.setDateTime(LocalDateTime.now().plusDays(7));
        d.setMaxParticipants(max);
        d.setCurrentParticipants(current);
        d.setStatus("SCHEDULED");
        return d;
    }

    private PaymentDto payment(String status) {
        PaymentDto p = new PaymentDto();
        p.setId(1L);
        p.setStatus(status);
        return p;
    }

    private FeignException.Conflict conflict() {
        Request request = Request.create(Request.HttpMethod.PATCH, "/api/classes/1/increment",
                new HashMap<>(), (byte[]) null, null);
        return new FeignException.Conflict("Conflict", request, null, new HashMap<>());
    }

    private BookingRequest req() {
        return new BookingRequest(7L, "john@example.com", "John Doe", 1L, 2);
    }

    @Test
    void cycleDeVieComplet_creationPuisConfirmation() throws Exception {
        when(classClient.getById(1L)).thenReturn(cls(10, 5));
        when(classClient.increment(1L, 2)).thenReturn(cls(10, 7));

        String response = mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.className").value("Yoga du matin"))
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(response).get("id").asLong();

        when(paymentClient.process(any())).thenReturn(payment("SUCCESS"));

        mockMvc.perform(patch("/api/bookings/{id}/confirm", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConfirmPaymentRequest("CREDIT_CARD", "1234", "txn_1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void create_plusDePlaces_retourne409() throws Exception {
        when(classClient.getById(1L)).thenReturn(cls(10, 9));
        when(classClient.increment(1L, 2)).thenThrow(conflict());

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req())))
                .andExpect(status().isConflict());
    }

    @Test
    void create_requeteInvalide_retourne400() throws Exception {
        BookingRequest invalid = new BookingRequest(null, "", "", 1L, 0);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancel_libereLesPlacesEtAnnule() throws Exception {
        when(classClient.getById(1L)).thenReturn(cls(10, 5));
        when(classClient.increment(1L, 2)).thenReturn(cls(10, 7));

        String response = mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(patch("/api/bookings/{id}/cancel", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
