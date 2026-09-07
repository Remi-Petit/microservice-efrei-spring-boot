package com.formation.book;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formation.book.dto.BookRequest;
import com.formation.book.model.Book;
import com.formation.book.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookRepository bookRepository;

    @BeforeEach
    void cleanDatabase() {
        bookRepository.deleteAll();
    }

    @Test
    void cycleDeVieComplet_creeEtRecupere() throws Exception {
        BookRequest createRequest = new BookRequest("978-2-07-061275-8", "Le Petit Prince", "Saint-Exupery", 3);

        String response = mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Le Petit Prince"))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/books/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value("978-2-07-061275-8"));
    }

    @Test
    void create_requeteInvalide_retourne400() throws Exception {
        BookRequest invalid = new BookRequest("", "", "", -1);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void borrow_livreSansExemplaire_retourne409() throws Exception {
        Book book = new Book("isbn", "Titre", "Auteur", 0);
        book = bookRepository.save(book);

        mockMvc.perform(post("/api/books/{id}/borrow", book.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void borrow_livreDisponible_decrementeLeStock() throws Exception {
        Book book = bookRepository.save(new Book("isbn", "Titre", "Auteur", 2));

        mockMvc.perform(post("/api/books/{id}/borrow", book.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCopies").value(1));
    }

    @Test
    void delete_livreExistant_retourne204() throws Exception {
        Book book = bookRepository.save(new Book("isbn", "Titre", "Auteur", 1));

        mockMvc.perform(delete("/api/books/{id}", book.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/books/{id}", book.getId()))
                .andExpect(status().isNotFound());
    }
}
