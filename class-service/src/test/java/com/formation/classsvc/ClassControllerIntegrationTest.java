package com.formation.classsvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formation.classsvc.dto.FitnessClassRequest;
import com.formation.classsvc.model.Category;
import com.formation.classsvc.model.Level;
import com.formation.classsvc.repository.FitnessClassRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ClassControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FitnessClassRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    private FitnessClassRequest request() {
        return new FitnessClassRequest("Yoga du matin", "Cours doux", "Marie", "Paris",
                Category.YOGA, Level.BEGINNER, 60, 20,
                new BigDecimal("20.00"), LocalDateTime.now().plusDays(7));
    }

    @Test
    void create_retourne201EtInitialiseLesParticipants() throws Exception {
        mockMvc.perform(post("/api/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentParticipants").value(0))
                .andExpect(jsonPath("$.maxParticipants").value(20))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    void create_requeteInvalide_retourne400() throws Exception {
        FitnessClassRequest invalid = new FitnessClassRequest("Yo", "", "Marie", "Paris",
                Category.YOGA, Level.BEGINNER, 60, 2,
                new BigDecimal("1.00"), LocalDateTime.now().plusDays(7));

        mockMvc.perform(post("/api/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void increment_reserveDesPlaces() throws Exception {
        String response = mockMvc.perform(post("/api/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(patch("/api/classes/{id}/increment", id).param("spots", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentParticipants").value(2));
    }

    @Test
    void increment_depasseLaCapacite_retourne409() throws Exception {
        String response = mockMvc.perform(post("/api/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FitnessClassRequest(
                                "Yoga du matin", "Cours doux", "Marie", "Paris",
                                Category.YOGA, Level.BEGINNER, 60, 5,
                                new BigDecimal("20.00"), LocalDateTime.now().plusDays(7)))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(patch("/api/classes/{id}/increment", id).param("spots", "6"))
                .andExpect(status().isConflict());
    }

    @Test
    void decrement_libereDesPlaces() throws Exception {
        String response = mockMvc.perform(post("/api/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(patch("/api/classes/{id}/increment", id).param("spots", "3"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/classes/{id}/decrement", id).param("spots", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentParticipants").value(2));
    }

    @Test
    void getById_retourneLeCours() throws Exception {
        mockMvc.perform(post("/api/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/classes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Yoga du matin"));
    }

    @Test
    void update_modifieLeCours() throws Exception {
        String response = mockMvc.perform(post("/api/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(put("/api/classes/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FitnessClassRequest(
                                "Yoga avance", "Cours intense", "Marie", "Lyon",
                                Category.YOGA, Level.ADVANCED, 90, 15,
                                new BigDecimal("35.00"), LocalDateTime.now().plusDays(10)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Yoga avance"))
                .andExpect(jsonPath("$.gymLocation").value("Lyon"));
    }

    @Test
    void getById_coursInexistant_retourne404() throws Exception {
        mockMvc.perform(get("/api/classes/9999"))
                .andExpect(status().isNotFound());
    }
}
