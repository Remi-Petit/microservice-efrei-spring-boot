package com.formation.payment;

import com.formation.payment.dto.PaymentRequest;
import com.formation.payment.dto.PaymentResponse;
import com.formation.payment.exception.PaymentForBookingNotFoundException;
import com.formation.payment.exception.PaymentNotRefundableException;
import com.formation.payment.model.Payment;
import com.formation.payment.model.PaymentMethod;
import com.formation.payment.model.PaymentStatus;
import com.formation.payment.repository.PaymentRepository;
import com.formation.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository repository;

    @InjectMocks
    private PaymentService service;

    private PaymentRequest request(BigDecimal amount) {
        return new PaymentRequest(1L, "BK-12345", 7L, amount,
                PaymentMethod.CREDIT_CARD, "1234", "txn_123456");
    }

    private Payment payment(PaymentStatus status) {
        return new Payment("PAY-AB123", 1L, "BK-12345", 7L,
                new BigDecimal("50.00"), PaymentMethod.CREDIT_CARD, "1234", "txn_123456",
                LocalDateTime.now(), status);
    }

    @Test
    void processPayment_montantSous100_accepte() {
        when(repository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse result = service.processPayment(request(new BigDecimal("50.00")));

        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(result.paymentReference()).startsWith("PAY-");
    }

    @Test
    void processPayment_montantEgale100_refuse() {
        when(repository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse result = service.processPayment(request(new BigDecimal("100.00")));

        assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void processPayment_montantDepasse100_refuse() {
        when(repository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse result = service.processPayment(request(new BigDecimal("150.00")));

        assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void refund_passeLeStatutARembourse() {
        Payment p = payment(PaymentStatus.SUCCESS);
        when(repository.findById(1L)).thenReturn(Optional.of(p));
        when(repository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse result = service.refund(1L);

        assertThat(result.status()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void refund_dejaRembourse_leveException() {
        when(repository.findById(1L)).thenReturn(Optional.of(payment(PaymentStatus.REFUNDED)));

        assertThatThrownBy(() -> service.refund(1L))
                .isInstanceOf(PaymentNotRefundableException.class);
    }

    @Test
    void refund_paiementEnEchec_leveException() {
        when(repository.findById(1L)).thenReturn(Optional.of(payment(PaymentStatus.FAILED)));

        assertThatThrownBy(() -> service.refund(1L))
                .isInstanceOf(PaymentNotRefundableException.class);
    }

    @Test
    void getByBookingId_aucunPaiement_leveException() {
        when(repository.findByBookingId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByBookingId(999L))
                .isInstanceOf(PaymentForBookingNotFoundException.class);
    }
}
