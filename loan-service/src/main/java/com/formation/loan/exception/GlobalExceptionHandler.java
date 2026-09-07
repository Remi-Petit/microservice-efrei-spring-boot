package com.formation.loan.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralise la gestion des exceptions pour renvoyer un format d'erreur uniforme.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LoanNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(LoanNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(build(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(BookNotFoundForLoanException.class)
    public ResponseEntity<Map<String, Object>> handleBookNotFound(BookNotFoundForLoanException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(build(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(BookServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleBookUnavailable(BookServiceUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(build(HttpStatus.BAD_GATEWAY, ex.getMessage()));
    }

    @ExceptionHandler({InsufficientCopiesForLoanException.class, LoanAlreadyReturnedException.class})
    public ResponseEntity<Map<String, Object>> handleConflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(build(HttpStatus.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> body = build(HttpStatus.BAD_REQUEST, "Erreur de validation");
        body.put("details", ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage(),
                        (a, b) -> a
                )));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(build(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne du serveur"));
    }

    private Map<String, Object> build(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return body;
    }
}
