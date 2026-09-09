package com.formation.payment.service;

import com.formation.payment.dto.PaymentRequest;
import com.formation.payment.dto.PaymentResponse;
import com.formation.payment.exception.PaymentForBookingNotFoundException;
import com.formation.payment.exception.PaymentNotFoundException;
import com.formation.payment.exception.PaymentNotRefundableException;
import com.formation.payment.mapper.PaymentMapper;
import com.formation.payment.model.Payment;
import com.formation.payment.model.PaymentStatus;
import com.formation.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PaymentService {

    /**
     * Seuil de la simulation de paiement : en dessous, le paiement est accepte ;
     * a partir de 100 EUR, il est refusé.
     */
    private static final BigDecimal REFUSAL_THRESHOLD = new BigDecimal("100.00");

    private final PaymentRepository repository;

    public PaymentService(PaymentRepository repository) {
        this.repository = repository;
    }

    /**
     * Traite un paiement (appele par booking-service) avec une simulation :
     * accepte tous les montants &lt; 100 EUR, refuse les montants >= 100 EUR.
     */
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        Payment payment = PaymentMapper.toEntity(request);
        payment.setStatus(simulate(request.amount()));
        return PaymentMapper.toResponse(repository.save(payment));
    }

    public PaymentResponse getByBookingId(Long bookingId) {
        Payment payment = repository.findByBookingId(bookingId)
                .orElseThrow(() -> new PaymentForBookingNotFoundException(bookingId));
        return PaymentMapper.toResponse(payment);
    }

    public List<PaymentResponse> getByUserId(Long userId) {
        return repository.findByUserIdOrderByPaymentDateDesc(userId).stream()
                .map(PaymentMapper::toResponse)
                .toList();
    }

    /**
     * Rembourse un paiement (annulation dans les delais). Un paiement deja
     * rembourse ou en echec ne peut pas etre rembourse.
     */
    @Transactional
    public PaymentResponse refund(Long id) {
        Payment payment = findPayment(id);
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new PaymentNotRefundableException("Le paiement " + id + " est deja rembourse");
        }
        if (payment.getStatus() == PaymentStatus.FAILED) {
            throw new PaymentNotRefundableException("Impossible de rembourser un paiement en echec (" + id + ")");
        }
        payment.setStatus(PaymentStatus.REFUNDED);
        return PaymentMapper.toResponse(repository.save(payment));
    }

    private Payment findPayment(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
    }

    private PaymentStatus simulate(BigDecimal amount) {
        return amount.compareTo(REFUSAL_THRESHOLD) >= 0 ? PaymentStatus.FAILED : PaymentStatus.SUCCESS;
    }
}
