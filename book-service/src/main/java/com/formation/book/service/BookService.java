package com.formation.book.service;

import com.formation.book.dto.BookRequest;
import com.formation.book.dto.BookResponse;
import com.formation.book.exception.BookNotFoundException;
import com.formation.book.exception.DuplicateIsbnException;
import com.formation.book.exception.InsufficientCopiesException;
import com.formation.book.exception.TooManyCopiesException;
import com.formation.book.mapper.BookMapper;
import com.formation.book.model.Book;
import com.formation.book.repository.BookRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    /**
     * Liste paginee et filtrable des livres.
     * Bonus : filtre par auteur et/ou titre (insensible a la casse) + pagination.
     */
    public Page<BookResponse> findAll(String author, String title, Pageable pageable) {
        return bookRepository.findAll(buildSpecification(author, title), pageable)
                .map(BookMapper::toResponse);
    }

    private Specification<Book> buildSpecification(String author, String title) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (author != null && !author.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("author")), "%" + author.toLowerCase() + "%"));
            }
            if (title != null && !title.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%"));
            }
            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public BookResponse findById(Long id) {
        return BookMapper.toResponse(findBook(id));
    }

    @Transactional
    public BookResponse create(BookRequest request) {
        if (bookRepository.existsByIsbn(request.isbn())) {
            throw new DuplicateIsbnException(request.isbn());
        }
        Book saved = bookRepository.save(BookMapper.toEntity(request));
        return BookMapper.toResponse(saved);
    }

    @Transactional
    public BookResponse update(Long id, BookRequest request) {
        Book book = findBook(id);
        // ISBN unique : on autorise le meme isbn uniquement pour le livre lui-meme.
        bookRepository.findByIsbn(request.isbn())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new DuplicateIsbnException(request.isbn());
                });
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
