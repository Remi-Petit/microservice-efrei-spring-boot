package com.formation.loan.exception;

import java.time.Instant;
import java.util.Map;

/**
 * Corps de reponse uniforme pour toutes les erreurs renvoyees par l'API.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, Object> details
) {
    public ApiError(Instant timestamp, int status, String error, String message) {
        this(timestamp, status, error, message, null);
    }
}
