package com.formation.loan.dto;

import com.formation.loan.model.LoanStatus;

import java.time.LocalDate;

public record LoanResponse(
        Long id,
        Long bookId,
        String bookTitle,
        String memberName,
        LocalDate loanDate,
        LocalDate dueDate,
        LocalDate returnDate,
        LoanStatus status
) {
}
