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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
                .andExpect(jsonPath("$.totalCopies").value(3))
                .andExpect(jsonPath("$.availableCopies").value(3))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/books/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value("978-2-07-061275-8"));
    }

    @Test
    void create_requeteInvalide_retourne400() throws Exception {
        BookRequest invalid = new BookRequest("", "", "", 0);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void decrementStock_livreSansExemplaire_retourne409() throws Exception {
        Book book = bookRepository.save(new Book("isbn", "Titre", "Auteur", 1, 0));

        mockMvc.perform(patch("/api/books/{id}/decrement-stock", book.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void decrementStock_livreDisponible_reduitLeStock() throws Exception {
        Book book = bookRepository.save(new Book("isbn", "Titre", "Auteur", 2, 2));

        mockMvc.perform(patch("/api/books/{id}/decrement-stock", book.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCopies").value(1));
    }

    @Test
    void incrementStock_neDepassePasLeTotal_retourne409() throws Exception {
        Book book = bookRepository.save(new Book("isbn", "Titre", "Auteur", 1, 1));

        mockMvc.perform(patch("/api/books/{id}/increment-stock", book.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_livreExistant_retourne204() throws Exception {
        Book book = bookRepository.save(new Book("isbn", "Titre", "Auteur", 1, 1));

        mockMvc.perform(delete("/api/books/{id}", book.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/books/{id}", book.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAll_filtreParAuteur_insensibleACasse() throws Exception {
        bookRepository.save(new Book("isbn-1", "Titre 1", "Camus", 3, 3));
        bookRepository.save(new Book("isbn-2", "Titre 2", "Hugo", 2, 2));

        mockMvc.perform(get("/api/books").param("author", "camus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].author").value("Camus"));
    }

    @Test
    void getAll_filtreParTitre() throws Exception {
        bookRepository.save(new Book("isbn-1", "La Peste", "Camus", 3, 3));
        bookRepository.save(new Book("isbn-2", "Les Miserables", "Hugo", 2, 2));

        mockMvc.perform(get("/api/books").param("title", "misera"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Les Miserables"));
    }

    @Test
    void getAll_pagination() throws Exception {
        bookRepository.save(new Book("isbn-1", "Titre 1", "Auteur 1", 1, 1));
        bookRepository.save(new Book("isbn-2", "Titre 2", "Auteur 2", 1, 1));
        bookRepository.save(new Book("isbn-3", "Titre 3", "Auteur 3", 1, 1));

        mockMvc.perform(get("/api/books").param("size", "2").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void create_isbnDuplique_retourne409() throws Exception {
        bookRepository.save(new Book("isbn-dup", "Titre 1", "Auteur", 1, 1));
        BookRequest duplicate = new BookRequest("isbn-dup", "Titre 2", "Auteur 2", 2);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict());
    }
}
