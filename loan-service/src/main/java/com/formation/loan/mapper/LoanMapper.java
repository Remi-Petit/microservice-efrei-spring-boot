package com.formation.loan.mapper;

import com.formation.loan.dto.LoanResponse;
import com.formation.loan.model.Loan;

public final class LoanMapper {

    private LoanMapper() {
    }

    public static LoanResponse toResponse(Loan loan) {
        if (loan == null) {
            return null;
        }
        return new LoanResponse(
                loan.getId(),
                loan.getBookId(),
                loan.getBookTitle(),
                loan.getBorrowerName(),
                loan.getLoanDate(),
                loan.getReturnDate(),
                loan.getStatus()
        );
    }
}
