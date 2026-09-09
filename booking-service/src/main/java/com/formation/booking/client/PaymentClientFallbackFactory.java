package com.formation.booking.client;

import com.formation.booking.dto.PaymentDto;
import com.formation.booking.dto.PaymentRequestDto;
import com.formation.booking.exception.BookingServiceUnavailableException;
import org.springframework.cloud.openfeign.FallbackFactory;

/**
 * Fallback du client payment-service (Circuit Breaker resilience4j).
 * <p>
 * Un paiement est une etape critique de la saga : on ne peut pas le "simuler".
 * En cas de panne du payment-service, on propage une indisponibilite (502) afin
 * que la reservation ne soit pas confirmee a tort.
 */
public class PaymentClientFallbackFactory implements FallbackFactory<PaymentClient> {

    @Override
    public PaymentClient create(Throwable cause) {
        return new PaymentClient() {
            @Override
            public PaymentDto process(PaymentRequestDto request) {
                throw new BookingServiceUnavailableException("payment-service indisponible (circuit breaker ouvert)");
            }

            @Override
            public PaymentDto getByBooking(Long bookingId) {
                throw new BookingServiceUnavailableException("payment-service indisponible (circuit breaker ouvert)");
            }

            @Override
            public PaymentDto refund(Long id) {
                throw new BookingServiceUnavailableException("payment-service indisponible (circuit breaker ouvert)");
            }
        };
    }
}
