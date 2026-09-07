package com.formation.book;

import com.formation.book.dto.BookRequest;
import com.formation.book.dto.BookResponse;
import com.formation.book.exception.BookNotFoundException;
import com.formation.book.exception.InsufficientCopiesException;
import com.formation.book.exception.TooManyCopiesException;
import com.formation.book.model.Book;
import com.formation.book.repository.BookRepository;
import com.formation.book.service.BookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    private Book book(String isbn, String title, String author, int total, int available) {
        Book b = new Book(isbn, title, author, total, available);
        b.setId(1L);
        return b;
    }

    @Test
    void findById_livreInexistant_leveBookNotFoundException() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.findById(99L))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_initialiseLeStockAuTotal() {
        BookRequest request = new BookRequest("978-2-07-061275-8", "Le Petit Prince", "Saint-Exupery", 3);
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
            Book b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        BookResponse result = bookService.create(request);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.isbn()).isEqualTo("978-2-07-061275-8");
        assertThat(result.totalCopies()).isEqualTo(3);
        assertThat(result.availableCopies()).isEqualTo(3);
    }

    @Test
    void decrementStock_reduitLeStock() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book("isbn", "Titre", "Auteur", 3, 3)));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookResponse result = bookService.decrementStock(1L);

        assertThat(result.availableCopies()).isEqualTo(2);
        assertThat(result.totalCopies()).isEqualTo(3);
    }

    @Test
    void decrementStock_stockEpuise_leveInsufficientCopiesException() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book("isbn", "Titre", "Auteur", 1, 0)));

        assertThatThrownBy(() -> bookService.decrementStock(1L))
                .isInstanceOf(InsufficientCopiesException.class);
    }

    @Test
    void incrementStock_ajouteUnExemplaire() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book("isbn", "Titre", "Auteur", 3, 2)));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookResponse result = bookService.incrementStock(1L);

        assertThat(result.availableCopies()).isEqualTo(3);
    }

    @Test
    void incrementStock_neDepasseJamaisLeTotal() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book("isbn", "Titre", "Auteur", 3, 3)));

        assertThatThrownBy(() -> bookService.incrementStock(1L))
                .isInstanceOf(TooManyCopiesException.class);
    }

    @Test
    void delete_livreInexistant_leveBookNotFoundException() {
        when(bookRepository.existsById(7L)).thenReturn(false);

        assertThatThrownBy(() -> bookService.delete(7L))
                .isInstanceOf(BookNotFoundException.class);

        verify(bookRepository, never()).deleteById(any(Long.class));
    }

    @Test
    void findAll_retournePageMappee() {
        Book b = book("isbn", "Titre", "Auteur", 3, 3);
        b.setId(1L);
        when(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(b)));

        Page<BookResponse> result = bookService.findAll(null, null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).isbn()).isEqualTo("isbn");
    }

    @Test
    void create_isbnDuplique_leveDuplicateIsbnException() {
        when(bookRepository.existsByIsbn("isbn-dup")).thenReturn(true);
        BookRequest request = new BookRequest("isbn-dup", "Titre", "Auteur", 2);

        assertThatThrownBy(() -> bookService.create(request))
                .isInstanceOf(com.formation.book.exception.DuplicateIsbnException.class);

        verify(bookRepository, never()).save(any(Book.class));
    }
}
