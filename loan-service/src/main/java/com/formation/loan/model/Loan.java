package com.formation.loan.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long bookId;

    // Snapshot du titre au moment de l'emprunt (le livre peut etre modifie ensuite)
    @Column(nullable = false)
    private String bookTitle;

    @Column(nullable = false)
    private String borrowerName;

    @Column(nullable = false)
    private Instant loanDate;

    private Instant returnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    public Loan() {
    }

    public Loan(Long bookId, String bookTitle, String borrowerName, Instant loanDate, LoanStatus status) {
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.borrowerName = borrowerName;
        this.loanDate = loanDate;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getBorrowerName() {
        return borrowerName;
    }

    public void setBorrowerName(String borrowerName) {
        this.borrowerName = borrowerName;
    }

    public Instant getLoanDate() {
        return loanDate;
    }

    public void setLoanDate(Instant loanDate) {
        this.loanDate = loanDate;
    }

    public Instant getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(Instant returnDate) {
        this.returnDate = returnDate;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }
}
