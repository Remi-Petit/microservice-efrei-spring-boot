package com.formation.booking.client;

import com.formation.booking.exception.BookingServiceUnavailableException;
import com.formation.booking.exception.InvalidBookingOperationException;
import com.formation.booking.exception.NoSpotsAvailableForBookingException;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClassClientFallbackFactoryTest {

    private final ClassClientFallbackFactory factory = new ClassClientFallbackFactory();

    private FeignException.NotFound notFound() {
        return new FeignException.NotFound("Not Found",
                Request.create(Request.HttpMethod.GET, "/api/classes/999", new HashMap<>(), (byte[]) null, null),
                null, new HashMap<>());
    }

    private FeignException.Conflict conflict() {
        return new FeignException.Conflict("Conflict",
                Request.create(Request.HttpMethod.PATCH, "/api/classes/1/increment", new HashMap<>(), (byte[]) null, null),
                null, new HashMap<>());
    }

    @Test
    void coursIntrouvable_leveInvalidBookingOperation() {
        ClassClient client = factory.create(notFound());
        assertThatThrownBy(() -> client.getById(999L))
                .isInstanceOf(InvalidBookingOperationException.class);
    }

    @Test
    void plusDePlaces_leveNoSpotsAvailableException() {
        ClassClient client = factory.create(conflict());
        assertThatThrownBy(() -> client.increment(1L, 2))
                .isInstanceOf(NoSpotsAvailableForBookingException.class);
    }

    @Test
    void serviceIndisponible_leveBookingServiceUnavailable() {
        ClassClient client = factory.create(new RuntimeException("service down"));
        assertThatThrownBy(() -> client.getById(1L))
                .isInstanceOf(BookingServiceUnavailableException.class);
        assertThatThrownBy(() -> client.decrement(1L, 2))
                .isInstanceOf(BookingServiceUnavailableException.class);
    }
}
