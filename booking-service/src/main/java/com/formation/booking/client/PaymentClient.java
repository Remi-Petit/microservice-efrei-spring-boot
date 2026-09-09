package com.formation.booking.client;

import com.formation.booking.dto.PaymentDto;
import com.formation.booking.dto.PaymentRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Contrat Feign vers payment-service : traitement et remboursement d'un paiement.
 */
@FeignClient(name = "payment-service", fallbackFactory = PaymentClientFallbackFactory.class)
public interface PaymentClient {

    @PostMapping("/api/payments")
    PaymentDto process(@RequestBody PaymentRequestDto request);

    @GetMapping("/api/payments/booking/{bookingId}")
    PaymentDto getByBooking(@PathVariable("bookingId") Long bookingId);

    @PostMapping("/api/payments/{id}/refund")
    PaymentDto refund(@PathVariable("id") Long id);
}
