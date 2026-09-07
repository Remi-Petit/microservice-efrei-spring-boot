package com.formation.book.controller;

import com.formation.book.dto.BookRequest;
import com.formation.book.dto.BookResponse;
import com.formation.book.service.BookService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public List<BookResponse> getAll() {
        return bookService.findAll();
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
    // metier (défense en profondeur).
    // =========================================================================

    @PostMapping("/{id}/borrow")
    public BookResponse borrow(@PathVariable Long id) {
        return bookService.borrow(id);
    }

    @PostMapping("/{id}/return")
    public BookResponse giveBack(@PathVariable Long id) {
        return bookService.giveBack(id);
    }
}
