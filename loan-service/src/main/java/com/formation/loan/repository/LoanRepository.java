package com.formation.loan.repository;

import com.formation.loan.model.Loan;
import com.formation.loan.model.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    long countByMemberNameAndStatus(String memberName, LoanStatus status);
}
