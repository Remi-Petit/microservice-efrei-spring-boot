package com.formation.loan;

import com.formation.loan.client.BookClient;
import com.formation.loan.dto.BookDto;
import com.formation.loan.dto.LoanRequest;
import com.formation.loan.dto.LoanResponse;
import com.formation.loan.exception.BookNotFoundForLoanException;
import com.formation.loan.exception.BookServiceUnavailableException;
import com.formation.loan.exception.InsufficientCopiesForLoanException;
import com.formation.loan.exception.LoanAlreadyReturnedException;
import com.formation.loan.exception.LoanNotFoundException;
import com.formation.loan.model.Loan;
import com.formation.loan.model.LoanStatus;
import com.formation.loan.repository.LoanRepository;
import com.formation.loan.service.LoanService;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookClient bookClient;

    @InjectMocks
    private LoanService loanService;

    private BookDto book(Long id, String title, int copies) {
        BookDto dto = new BookDto();
        dto.setId(id);
        dto.setTitle(title);
        dto.setAvailableCopies(copies);
        return dto;
    }

    private FeignException.NotFound notFound() {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "/api/books/999",
                new HashMap<>(),
                (byte[]) null,
                null
        );
        return new FeignException.NotFound("Not Found", request, null, new HashMap<>());
    }

    private FeignException conflict() {
        Request request = Request.create(
                Request.HttpMethod.PATCH,
                "/api/books/1/decrement-stock",
                new HashMap<>(),
                (byte[]) null,
                null
        );
        return new FeignException.Conflict("Conflict", request, null, new HashMap<>());
    }

    private FeignException feignError() {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "/api/books/1",
                new HashMap<>(),
                (byte[]) null,
                null
        );
        return new FeignException.ServiceUnavailable("Service Unavailable", request, null, new HashMap<>());
    }

    @Test
    void create_livreDisponible_decrementeEtSauvegarde() {
        when(bookClient.getBookById(eq(1L))).thenReturn(book(1L, "Le Petit Prince", 3));
        when(bookClient.decrementStock(1L)).thenReturn(book(1L, "Le Petit Prince", 2));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan l = invocation.getArgument(0);
            return l;
        });

        LoanRequest request = new LoanRequest(1L, "Alice");
        LoanResponse result = loanService.create(request);

        verify(bookClient).decrementStock(1L);
        assertThat(result.status()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(result.memberName()).isEqualTo("Alice");
    }

    @Test
    void create_calculeDueDateAPlus14Jours() {
        when(bookClient.getBookById(eq(1L))).thenReturn(book(1L, "Le Petit Prince", 3));
        when(bookClient.decrementStock(1L)).thenReturn(book(1L, "Le Petit Prince", 2));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanResponse result = loanService.create(new LoanRequest(1L, "Alice"));

        LocalDate expectedDueDate = LocalDate.now().plusDays(14);
        assertThat(result.loanDate()).isEqualTo(LocalDate.now());
        assertThat(result.dueDate()).isEqualTo(expectedDueDate);
    }

    @Test
    void create_aucunExemplaire_leveInsufficientCopiesForLoanException() {
        when(bookClient.getBookById(eq(1L))).thenReturn(book(1L, "Le Petit Prince", 0));

        assertThatThrownBy(() -> loanService.create(new LoanRequest(1L, "Alice")))
                .isInstanceOf(InsufficientCopiesForLoanException.class);

        verify(bookClient, never()).decrementStock(any(Long.class));
    }

    @Test
    void create_livreInexistantChezBookService_leveBookNotFound() {
        when(bookClient.getBookById(eq(999L))).thenThrow(notFound());

        assertThatThrownBy(() -> loanService.create(new LoanRequest(999L, "Alice")))
                .isInstanceOf(BookNotFoundForLoanException.class);
    }

    @Test
    void create_bookServiceIndisponible_leveServiceUnavailable() {
        when(bookClient.getBookById(eq(1L))).thenThrow(feignError());

        assertThatThrownBy(() -> loanService.create(new LoanRequest(1L, "Alice")))
                .isInstanceOf(BookServiceUnavailableException.class);
    }

    @Test
    void create_casDeConcurrence_bookServiceRenvoie409_leveInsufficientCopies() {
        // TOCTOU : meme si la lecture (etape 1) voit encore un exemplaire, un
        // emprunt concurrent a pu epuiser le stock avant le decrement (etape 2).
        when(bookClient.getBookById(eq(1L))).thenReturn(book(1L, "Le Petit Prince", 1));
        when(bookClient.decrementStock(1L)).thenThrow(conflict());

        assertThatThrownBy(() -> loanService.create(new LoanRequest(1L, "Alice")))
                .isInstanceOf(InsufficientCopiesForLoanException.class);
    }

    @Test
    void giveBack_reincrementeEtMarqueRendu() {
        Loan loan = new Loan("Alice", 1L, "Le Petit Prince",
                LocalDate.now(), LocalDate.now().plusDays(14), LoanStatus.ACTIVE);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(bookClient.incrementStock(1L)).thenReturn(book(1L, "Le Petit Prince", 2));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        loanService.giveBack(1L);

        verify(bookClient).incrementStock(1L);
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.RETURNED);
        assertThat(loan.getReturnDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void giveBack_dejaRendu_leveLoanAlreadyReturnedException() {
        Loan loan = new Loan("Alice", 1L, "Le Petit Prince",
                LocalDate.now(), LocalDate.now().plusDays(14), LoanStatus.RETURNED);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.giveBack(1L))
                .isInstanceOf(LoanAlreadyReturnedException.class);
    }

    @Test
    void findById_empruntInexistant_leveLoanNotFoundException() {
        when(loanRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.findById(7L))
                .isInstanceOf(LoanNotFoundException.class);
    }
}
