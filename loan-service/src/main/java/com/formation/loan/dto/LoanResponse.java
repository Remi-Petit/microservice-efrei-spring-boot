package com.formation.loan.dto;

import com.formation.loan.model.LoanStatus;

import java.time.Instant;

public record LoanResponse(
        Long id,
        Long bookId,
        String bookTitle,
        String borrowerName,
        Instant loanDate,
        Instant returnDate,
        LoanStatus status
) {
}
