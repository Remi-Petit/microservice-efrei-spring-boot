package com.formation.loan.controller;

import com.formation.loan.dto.LoanRequest;
import com.formation.loan.dto.LoanResponse;
import com.formation.loan.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping
    public List<LoanResponse> getAll() {
        return loanService.findAll();
    }

    @GetMapping("/{id}")
    public LoanResponse getById(@PathVariable Long id) {
        return loanService.findById(id);
    }

    @PostMapping
    public ResponseEntity<LoanResponse> create(@Valid @RequestBody LoanRequest request) {
        LoanResponse created = loanService.create(request);
        return ResponseEntity.created(URI.create("/api/loans/" + created.id())).body(created);
    }

    @PatchMapping("/{id}/return")
    public LoanResponse giveBack(@PathVariable Long id) {
        return loanService.giveBack(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        loanService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
