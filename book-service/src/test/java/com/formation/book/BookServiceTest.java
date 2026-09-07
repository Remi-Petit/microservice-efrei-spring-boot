package com.formation.book;

import com.formation.book.dto.BookRequest;
import com.formation.book.dto.BookResponse;
import com.formation.book.exception.BookNotFoundException;
import com.formation.book.exception.InsufficientCopiesException;
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

    @Test
    void findById_livreInexistant_leveBookNotFoundException() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.findById(99L))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_sauvegardeEtRetourneLeLivreCree() {
        BookRequest request = new BookRequest("978-2-07-061275-8", "Le Petit Prince", "Saint-Exupery", 3);
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
            Book b = invocation.getArgument(0);
            b.setId(1L);
            return b;
        });

        BookResponse result = bookService.create(request);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.isbn()).isEqualTo("978-2-07-061275-8");
        assertThat(result.availableCopies()).isEqualTo(3);
    }

    @Test
    void borrow_decrementeLeStock() {
        Book book = new Book("isbn", "Titre", "Auteur", 2);
        book.setId(1L);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookResponse result = bookService.borrow(1L);

        assertThat(result.availableCopies()).isEqualTo(1);
    }

    @Test
    void borrow_plusAucunExemplaire_leveInsufficientCopiesException() {
        Book book = new Book("isbn", "Titre", "Auteur", 0);
        book.setId(1L);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> bookService.borrow(1L))
                .isInstanceOf(InsufficientCopiesException.class);
    }

    @Test
    void giveBack_reincrementeLeStock() {
        Book book = new Book("isbn", "Titre", "Auteur", 0);
        book.setId(1L);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookResponse result = bookService.giveBack(1L);

        assertThat(result.availableCopies()).isEqualTo(1);
    }

    @Test
    void delete_livreInexistant_leveBookNotFoundException() {
        when(bookRepository.existsById(7L)).thenReturn(false);

        assertThatThrownBy(() -> bookService.delete(7L))
                .isInstanceOf(BookNotFoundException.class);

        verify(bookRepository, never()).deleteById(any(Long.class));
    }
}
