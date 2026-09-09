package com.formation.payment.controller;

import com.formation.payment.dto.PaymentRequest;
import com.formation.payment.dto.PaymentResponse;
import com.formation.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** Traite un paiement (appele par booking-service). */
    @PostMapping
    public ResponseEntity<PaymentResponse> process(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse created = paymentService.processPayment(request);
        return ResponseEntity.created(URI.create("/api/payments/" + created.id())).body(created);
    }

    /** Recupere le paiement associe a une reservation. */
    @GetMapping("/booking/{bookingId}")
    public PaymentResponse getByBooking(@PathVariable Long bookingId) {
        return paymentService.getByBookingId(bookingId);
    }

    /** Rembourse un paiement (annulation dans les delais). */
    @PostMapping("/{id}/refund")
    public PaymentResponse refund(@PathVariable Long id) {
        return paymentService.refund(id);
    }

    /** Historique des paiements d'un utilisateur. */
    @GetMapping("/user/{userId}")
    public List<PaymentResponse> getByUser(@PathVariable Long userId) {
        return paymentService.getByUserId(userId);
    }
}
