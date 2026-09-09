package com.formation.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formation.notification.dto.NotificationRequest;
import com.formation.notification.model.NotificationStatus;
import com.formation.notification.model.NotificationType;
import com.formation.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    private NotificationRequest request(String email) {
        return new NotificationRequest(1L, email, NotificationType.PAYMENT_CONFIRMATION,
                "Paiement confirme", "Votre paiement a ete accepte.");
    }

    @Test
    void send_emailValide_retourne201EtSENT() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("john@example.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.type").value("PAYMENT_CONFIRMATION"));
    }

    @Test
    void send_emailInvalide_retourneFAILED() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("sans-arobase"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void send_requeteInvalide_retourne400() throws Exception {
        NotificationRequest invalid = new NotificationRequest(null, null, null, "", "");

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void retry_relanceUneNotificationEnEchec() throws Exception {
        String response = mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("sans-arobase"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(patch("/api/notifications/{id}/retry", id))
                .andExpect(status().isOk());
    }

    @Test
    void getByUser_retourneLHistorique() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("john@example.com"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/notifications/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SENT"));
    }
}
