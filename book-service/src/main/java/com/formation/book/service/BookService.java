package com.formation.book.service;

import com.formation.book.dto.BookRequest;
import com.formation.book.dto.BookResponse;
import com.formation.book.exception.BookNotFoundException;
import com.formation.book.exception.InsufficientCopiesException;
import com.formation.book.exception.TooManyCopiesException;
import com.formation.book.mapper.BookMapper;
import com.formation.book.model.Book;
import com.formation.book.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<BookResponse> findAll() {
        return bookRepository.findAll().stream()
                .map(BookMapper::toResponse)
                .toList();
    }

    public BookResponse findById(Long id) {
        return BookMapper.toResponse(findBook(id));
    }

    @Transactional
    public BookResponse create(BookRequest request) {
        Book saved = bookRepository.save(BookMapper.toEntity(request));
        return BookMapper.toResponse(saved);
    }

    @Transactional
    public BookResponse update(Long id, BookRequest request) {
        Book book = findBook(id);
        book.setIsbn(request.isbn());
        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setTotalCopies(request.totalCopies());
        // On ne reduit jamais le nombre disponible au-dela du nouveau total.
        if (book.getAvailableCopies() > request.totalCopies()) {
            book.setAvailableCopies(request.totalCopies());
        }
        return BookMapper.toResponse(bookRepository.save(book));
    }

    @Transactional
    public void delete(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new BookNotFoundException(id);
        }
        bookRepository.deleteById(id);
    }

    /**
     * Décrémente le nombre d'exemplaires disponibles lors d'un emprunt.
     * RE-VERIFIE la disponibilité : ne fait jamais confiance à l'appelant
     * (défense en profondeur, meme s'il s'agit d'un microservice interne).
     * C'est le volet "use" du probleme TOCTOU : la re-verification evite qu'un
     * emprunt concurrent ne consomme le dernier exemplaire entre le check et ici.
     */
    @Transactional
    public BookResponse decrementStock(Long id) {
        Book book = findBook(id);
        if (book.getAvailableCopies() <= 0) {
            throw new InsufficientCopiesException(id);
        }
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        return BookMapper.toResponse(bookRepository.save(book));
    }

    /**
     * Réincrémente le nombre d'exemplaires disponibles lors d'un retour,
     * sans jamais depasser le nombre total.
     */
    @Transactional
    public BookResponse incrementStock(Long id) {
        Book book = findBook(id);
        if (book.getAvailableCopies() >= book.getTotalCopies()) {
            throw new TooManyCopiesException(id);
        }
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        return BookMapper.toResponse(bookRepository.save(book));
    }

    private Book findBook(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
    }
}
