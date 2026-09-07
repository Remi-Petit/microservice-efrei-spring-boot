package com.formation.loan.service;

import com.formation.loan.client.BookClient;
import com.formation.loan.dto.BookDto;
import com.formation.loan.dto.LoanRequest;
import com.formation.loan.dto.LoanResponse;
import com.formation.loan.exception.BookNotFoundForLoanException;
import com.formation.loan.exception.BookServiceUnavailableException;
import com.formation.loan.exception.InsufficientCopiesForLoanException;
import com.formation.loan.exception.LoanAlreadyReturnedException;
import com.formation.loan.exception.LoanNotFoundException;
import com.formation.loan.mapper.LoanMapper;
import com.formation.loan.model.Loan;
import com.formation.loan.model.LoanStatus;
import com.formation.loan.repository.LoanRepository;
import feign.FeignException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookClient bookClient;

    public LoanService(LoanRepository loanRepository, BookClient bookClient) {
        this.loanRepository = loanRepository;
        this.bookClient = bookClient;
    }

    public List<LoanResponse> findAll() {
        return loanRepository.findAll().stream()
                .map(LoanMapper::toResponse)
                .toList();
    }

    public LoanResponse findById(Long id) {
        return LoanMapper.toResponse(findLoan(id));
    }

    /**
     * Cree un emprunt.
     *
     * DEFENSE EN PROFONDEUR (2 niveaux) :
     *  1. Ici, on LIT le livre et on verifie la disponibilite avant d'appeler.
     *  2. Puis on appelle book-service qui RE-VERIFIE a nouveau avant de
     *     decrémenter son propre stock (on ne fait jamais confiance a un
     *     appelant, meme interne).
     */
    @Transactional
    public LoanResponse create(LoanRequest request) {
        Long bookId = request.bookId();

        // Couche 1 : verifier la disponibilite avant l'appel (pré-contrôle)
        BookDto book = fetchBook(bookId);
        if (book.getAvailableCopies() <= 0) {
            throw new InsufficientCopiesForLoanException(bookId);
        }

        // Couche 2 : book-service re-verifie et decremente le stock.
        // Si plus aucun exemplaire, book-service renvoie 409 -> FeignException.Conflict
        // -> traduit en InsufficientCopiesForLoanException (409).
        try {
            bookClient.borrowBook(bookId);
        } catch (FeignException.Conflict ex) {
            throw new InsufficientCopiesForLoanException(bookId);
        } catch (FeignException.NotFound ex) {
            throw new BookNotFoundForLoanException(bookId);
        } catch (FeignException ex) {
            throw new BookServiceUnavailableException(ex);
        }

        Loan loan = new Loan(book.getId(), book.getTitle(), request.borrowerName(), Instant.now(), LoanStatus.BORROWED);
        return LoanMapper.toResponse(loanRepository.save(loan));
    }

    /**
     * Rendre un emprunt : re-incremente le stock cote book-service puis marque
     * l'emprunt comme RETOURNE.
     */
    @Transactional
    public LoanResponse giveBack(Long id) {
        Loan loan = findLoan(id);
        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new LoanAlreadyReturnedException(id);
        }

        try {
            bookClient.returnBook(loan.getBookId());
        } catch (FeignException.NotFound ex) {
            throw new BookNotFoundForLoanException(loan.getBookId());
        } catch (FeignException ex) {
            throw new BookServiceUnavailableException(ex);
        }

        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnDate(Instant.now());
        return LoanMapper.toResponse(loanRepository.save(loan));
    }

    @Transactional
    public void delete(Long id) {
        if (!loanRepository.existsById(id)) {
            throw new LoanNotFoundException(id);
        }
        loanRepository.deleteById(id);
    }

    private Loan findLoan(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new LoanNotFoundException(id));
    }

    /**
     * Traduit les erreurs Feign de LECTURE en exceptions metier.
     */
    private BookDto fetchBook(Long bookId) {
        try {
            return bookClient.getBookById(bookId);
        } catch (FeignException.NotFound ex) {
            throw new BookNotFoundForLoanException(bookId);
        } catch (FeignException ex) {
            throw new BookServiceUnavailableException(ex);
        }
    }
}
