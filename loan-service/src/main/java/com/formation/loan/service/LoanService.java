package com.formation.loan.service;

import com.formation.loan.client.BookClient;
import com.formation.loan.dto.BookDto;
import com.formation.loan.dto.LoanRequest;
import com.formation.loan.dto.LoanResponse;
import com.formation.loan.exception.ActiveLoanLimitExceededException;
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

import java.time.LocalDate;
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
     * Probleme TOCTOU (Time-Of-Check to Time-Of-Use) : entre l'etape 1 (verifier
     * la disponibilite) et l'etape 2 (decrémenter), un autre emprunt concurrent
     * peut avoir consomme le dernier exemplaire. C'est pourquoi book-service
     * RE-VERIFIE la condition au moment de decrémenter (defense en profondeur).
     */
    @Transactional
    public LoanResponse create(LoanRequest request) {
        Long bookId = request.bookId();

        // ---- Etape 0 : limite de 3 emprunts ACTIVE simultanes par membre (bonus) ----
        if (loanRepository.countByMemberNameAndStatus(request.memberName(), LoanStatus.ACTIVE) >= 3) {
            throw new ActiveLoanLimitExceededException(request.memberName());
        }

        // ---- Etape 1 : LECTURE et verification prealable (le "check") ----
        BookDto book = fetchBook(bookId);
        if (book.getAvailableCopies() <= 0) {
            throw new InsufficientCopiesForLoanException(bookId);
        }

        // ---- Etape 2 : ECRITURE (le "use") ----
        // book-service re-verifie le stock avant de decrementer. S'il n'y a plus
        // d'exemplaire (concurrence), il repond 409 -> FeignException.Conflict.
        try {
            bookClient.decrementStock(bookId);
        } catch (FeignException.Conflict ex) {
            throw new InsufficientCopiesForLoanException(bookId);
        } catch (FeignException.NotFound ex) {
            throw new BookNotFoundForLoanException(bookId);
        } catch (FeignException ex) {
            throw new BookServiceUnavailableException(ex);
        }

        // ---- Etape 3 : creation du snapshot (titre copie) + dueDate = +14 jours ----
        LocalDate loanDate = LocalDate.now();
        Loan loan = new Loan(
                request.memberName(),
                book.getId(),
                book.getTitle(),
                loanDate,
                loanDate.plusDays(14),
                LoanStatus.ACTIVE
        );
        return LoanMapper.toResponse(loanRepository.save(loan));
    }

    /**
     * Rendre un emprunt : verifie qu'il est encore ACTIVE, re-incremente le stock
     * cote book-service puis marque l'emprunt comme RETOURNE.
     */
    @Transactional
    public LoanResponse giveBack(Long id) {
        Loan loan = findLoan(id);
        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new LoanAlreadyReturnedException(id);
        }

        try {
            bookClient.incrementStock(loan.getBookId());
        } catch (FeignException.NotFound ex) {
            throw new BookNotFoundForLoanException(loan.getBookId());
        } catch (FeignException ex) {
            throw new BookServiceUnavailableException(ex);
        }

        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnDate(LocalDate.now());
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
