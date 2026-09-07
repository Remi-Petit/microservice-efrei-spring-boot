package com.formation.loan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formation.loan.client.BookClient;
import com.formation.loan.dto.BookDto;
import com.formation.loan.dto.LoanRequest;
import com.formation.loan.model.Loan;
import com.formation.loan.model.LoanStatus;
import com.formation.loan.repository.LoanRepository;
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

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LoanControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LoanRepository loanRepository;

    @MockBean
    private BookClient bookClient;

    @BeforeEach
    void cleanDatabase() {
        loanRepository.deleteAll();
    }

    private BookDto book(Long id, String title, int copies) {
        BookDto dto = new BookDto();
        dto.setId(id);
        dto.setTitle(title);
        dto.setAvailableCopies(copies);
        return dto;
    }

    private FeignException.NotFound notFound() {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "/api/books/999",
                new HashMap<>(),
                (byte[]) null,
                null
        );
        return new FeignException.NotFound("Not Found", request, null, new HashMap<>());
    }

    @Test
    void cycleDeVieComplet_creerEtRecuperer() throws Exception {
        when(bookClient.getBookById(eq(1L))).thenReturn(book(1L, "Le Petit Prince", 3));
        when(bookClient.decrementStock(1L)).thenReturn(book(1L, "Le Petit Prince", 2));

        LoanRequest createRequest = new LoanRequest(1L, "Alice");

        String response = mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookTitle").value("Le Petit Prince"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.dueDate").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/loans/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberName").value("Alice"));
    }

    @Test
    void create_aucunExemplaire_retourne409() throws Exception {
        when(bookClient.getBookById(eq(1L))).thenReturn(book(1L, "Le Petit Prince", 0));

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoanRequest(1L, "Alice"))))
                .andExpect(status().isConflict());
    }

    @Test
    void create_livreInexistantChezBookService_retourne400() throws Exception {
        when(bookClient.getBookById(eq(999L))).thenThrow(notFound());

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoanRequest(999L, "Alice"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_requeteInvalide_retourne400() throws Exception {
        LoanRequest invalid = new LoanRequest(null, "");

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void return_marqueLEmpruntCommeRendu() throws Exception {
        Loan loan = new Loan("Alice", 1L, "Le Petit Prince",
                java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(14), LoanStatus.ACTIVE);
        Loan saved = loanRepository.save(loan);
        when(bookClient.incrementStock(1L)).thenReturn(book(1L, "Le Petit Prince", 2));

        mockMvc.perform(patch("/api/loans/{id}/return", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.returnDate").isNotEmpty());
    }

    @Test
    void return_dejaRendu_retourne409() throws Exception {
        Loan loan = new Loan("Alice", 1L, "Le Petit Prince",
                java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(14), LoanStatus.RETURNED);
        loan.setReturnDate(java.time.LocalDate.now());
        Loan saved = loanRepository.save(loan);

        mockMvc.perform(patch("/api/loans/{id}/return", saved.getId()))
                .andExpect(status().isConflict());
    }
}
