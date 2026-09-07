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
}
