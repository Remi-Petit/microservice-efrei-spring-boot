package com.formation.booking.client;

import com.formation.booking.dto.FitnessClassDto;
import com.formation.booking.exception.BookingServiceUnavailableException;
import com.formation.booking.exception.InvalidBookingOperationException;
import com.formation.booking.exception.NoSpotsAvailableForBookingException;
import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;

/**
 * Fallback du client class-service (Circuit Breaker resilience4j).
 * <p>
 * Transforme les erreurs Feign en exceptions metier afin de preserver les
 * regles de la saga : 404 -> cours introuvable, 409 -> plus de places,
 * toute autre erreur -> service indisponible (circuit ouvert).
 */
public class ClassClientFallbackFactory implements FallbackFactory<ClassClient> {

    @Override
    public ClassClient create(Throwable cause) {
        return new ClassClient() {
            @Override
            public FitnessClassDto getById(Long id) {
                throw mapError(cause, id);
            }

            @Override
            public FitnessClassDto increment(Long id, int spots) {
                throw mapError(cause, id);
            }

            @Override
            public FitnessClassDto decrement(Long id, int spots) {
                throw mapError(cause, id);
            }
        };
    }

    private RuntimeException mapError(Throwable cause, Long classId) {
        if (cause instanceof FeignException.NotFound) {
            return new InvalidBookingOperationException("Le cours " + classId + " n'existe pas");
        }
        if (cause instanceof FeignException.Conflict) {
            return new NoSpotsAvailableForBookingException(classId);
        }
        return new BookingServiceUnavailableException("class-service indisponible (circuit breaker ouvert)");
    }
}
