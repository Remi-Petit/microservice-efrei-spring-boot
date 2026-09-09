package com.formation.booking.client;

import com.formation.booking.dto.PaymentRequestDto;
import com.formation.booking.exception.BookingServiceUnavailableException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentClientFallbackFactoryTest {

    private final PaymentClientFallbackFactory factory = new PaymentClientFallbackFactory();

    @Test
    void process_leveBookingServiceUnavailable() {
        PaymentClient client = factory.create(new RuntimeException("payment down"));
        assertThatThrownBy(() -> client.process(new PaymentRequestDto(
                1L, "BK-1", 7L, new BigDecimal("40.00"), "CREDIT_CARD", "1234", "txn")))
                .isInstanceOf(BookingServiceUnavailableException.class);
    }

    @Test
    void refund_leveBookingServiceUnavailable() {
        PaymentClient client = factory.create(new RuntimeException("payment down"));
        assertThatThrownBy(() -> client.refund(1L))
                .isInstanceOf(BookingServiceUnavailableException.class);
        assertThatThrownBy(() -> client.getByBooking(1L))
                .isInstanceOf(BookingServiceUnavailableException.class);
    }
}
