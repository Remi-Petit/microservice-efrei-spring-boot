package com.formation.classsvc.controller;

import com.formation.classsvc.dto.FitnessClassRequest;
import com.formation.classsvc.dto.FitnessClassResponse;
import com.formation.classsvc.model.Category;
import com.formation.classsvc.model.Level;
import com.formation.classsvc.service.FitnessClassService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/classes")
public class ClassController {

    private final FitnessClassService classService;

    public ClassController(FitnessClassService classService) {
        this.classService = classService;
    }

    @GetMapping
    public Page<FitnessClassResponse> getAll(
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Level level,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String instructor,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            Pageable pageable) {
        return classService.findAll(category, level, location, instructor, dateFrom, dateTo, pageable);
    }

    @GetMapping("/search")
    public Page<FitnessClassResponse> search(
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Level level,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String instructor,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            Pageable pageable) {
        return classService.findAll(category, level, location, instructor, dateFrom, dateTo, pageable);
    }

    @GetMapping("/{id}")
    public FitnessClassResponse getById(@PathVariable Long id) {
        return classService.findById(id);
    }

    @PostMapping
    public ResponseEntity<FitnessClassResponse> create(@Valid @RequestBody FitnessClassRequest request) {
        FitnessClassResponse created = classService.create(request);
        return ResponseEntity.created(URI.create("/api/classes/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public FitnessClassResponse update(@PathVariable Long id, @Valid @RequestBody FitnessClassRequest request) {
        return classService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        classService.cancel(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // =========================================================================
    // Endpoints d'ECRITURE reserves a booking-service (appel interne via Feign).
    // Ils modifient le compteur de participants et re-verifient la regle metier
    // (verrouillage optimiste, volet "use" du probleme TOCTOU).
    // =========================================================================

    @PatchMapping("/{id}/increment")
    public FitnessClassResponse increment(@PathVariable Long id,
                                          @RequestParam(defaultValue = "1") int spots) {
        return classService.incrementParticipants(id, spots);
    }

    @PatchMapping("/{id}/decrement")
    public FitnessClassResponse decrement(@PathVariable Long id,
                                          @RequestParam(defaultValue = "1") int spots) {
        return classService.decrementParticipants(id, spots);
    }
}
