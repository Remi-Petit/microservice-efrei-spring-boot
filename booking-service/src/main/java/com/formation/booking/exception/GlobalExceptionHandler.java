package com.formation.booking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralise la gestion des exceptions pour renvoyer un corps d'erreur uniforme
 * ({@link ApiError}), dans le meme esprit que les autres services du cours.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(BookingNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(build(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(BookingServiceUnavailableException.class)
    public ResponseEntity<ApiError> handleUnavailable(BookingServiceUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(build(HttpStatus.BAD_GATEWAY, ex.getMessage()));
    }

    @ExceptionHandler({NoSpotsAvailableForBookingException.class, InvalidBookingOperationException.class, PaymentExpiredException.class})
    public ResponseEntity<ApiError> handleConflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(build(HttpStatus.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> details = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage(),
                        (a, b) -> a
                ));
        ApiError error = new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(), "Erreur de validation", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(build(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne du serveur"));
    }

    private ApiError build(HttpStatus status, String message) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message);
    }
}
