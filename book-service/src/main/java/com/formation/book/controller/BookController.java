package com.formation.book.controller;

import com.formation.book.dto.BookRequest;
import com.formation.book.dto.BookResponse;
import com.formation.book.service.BookService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public Page<BookResponse> getAll(
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String title,
            Pageable pageable) {
        return bookService.findAll(author, title, pageable);
    }

    @GetMapping("/{id}")
    public BookResponse getById(@PathVariable Long id) {
        return bookService.findById(id);
    }

    @PostMapping
    public ResponseEntity<BookResponse> create(@Valid @RequestBody BookRequest request) {
        BookResponse created = bookService.create(request);
        return ResponseEntity.created(URI.create("/api/books/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public BookResponse update(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
        return bookService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // =========================================================================
    // Endpoints d'ECRITURE reserves a loan-service (appel interne via Feign).
    // Ce ne sont pas des operations destinees a un client externe : elles
    // modifient le stock selon l'etat du livre, et re-verifient la regle
    // metier (défense en profondeur, volet "use" du probleme TOCTOU).
    // =========================================================================

    @PatchMapping("/{id}/decrement-stock")
    public BookResponse decrementStock(@PathVariable Long id) {
        return bookService.decrementStock(id);
    }

    @PatchMapping("/{id}/increment-stock")
    public BookResponse incrementStock(@PathVariable Long id) {
        return bookService.incrementStock(id);
    }
}
