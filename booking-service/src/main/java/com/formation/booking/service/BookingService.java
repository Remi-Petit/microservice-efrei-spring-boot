package com.formation.booking.service;

import com.formation.booking.client.ClassClient;
import com.formation.booking.client.NotificationClient;
import com.formation.booking.client.PaymentClient;
import com.formation.booking.dto.BookingRequest;
import com.formation.booking.dto.BookingResponse;
import com.formation.booking.dto.ConfirmPaymentRequest;
import com.formation.booking.dto.FitnessClassDto;
import com.formation.booking.dto.NotificationRequestDto;
import com.formation.booking.dto.PaymentDto;
import com.formation.booking.dto.PaymentRequestDto;
import com.formation.booking.exception.BookingNotFoundException;
import com.formation.booking.exception.BookingServiceUnavailableException;
import com.formation.booking.exception.InvalidBookingOperationException;
import com.formation.booking.exception.NoSpotsAvailableForBookingException;
import com.formation.booking.exception.PaymentExpiredException;
import com.formation.booking.mapper.BookingMapper;
import com.formation.booking.model.Booking;
import com.formation.booking.model.BookingStatus;
import com.formation.booking.repository.BookingRepository;
import com.formation.booking.util.ReferenceGenerator;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Orchestration de la reservation (pattern <b>Saga</b>) :
 * <ol>
 *   <li>lecture du cours (check) ;</li>
 *   <li>reservation des places (use -> re-verifiee cote class-service) ;</li>
 *   <li>creation de la reservation + notification ;</li>
 *   <li>confirmation apres paiement (payment-service).</li>
 * </ol>
 * Chaque etape peut echouer ; en cas d'echec apres reservation, une
 * <b>compensation</b> libere les places deja prises.
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository repository;
    private final ClassClient classClient;
    private final PaymentClient paymentClient;
    private final NotificationClient notificationClient;

    public BookingService(BookingRepository repository, ClassClient classClient,
                          PaymentClient paymentClient, NotificationClient notificationClient) {
        this.repository = repository;
        this.classClient = classClient;
        this.paymentClient = paymentClient;
        this.notificationClient = notificationClient;
    }

    public List<BookingResponse> findAll() {
        return repository.findAll().stream().map(BookingMapper::toResponse).toList();
    }

    public BookingResponse findById(Long id) {
        return BookingMapper.toResponse(findBooking(id));
    }

    public List<BookingResponse> findByUser(Long userId) {
        return repository.findByUserIdOrderByBookingDateDesc(userId).stream()
                .map(BookingMapper::toResponse)
                .toList();
    }

    /** Reservations en attente de paiement dont le delai est expire. */
    public List<BookingResponse> findExpired() {
        return repository.findByStatusAndPaymentDeadlineBefore(BookingStatus.PENDING_PAYMENT, LocalDateTime.now())
                .stream().map(BookingMapper::toResponse).toList();
    }

    // =========================================================================
    // Cas 1 : reservation reussie (et cas 2 : plus de places)
    // =========================================================================
    @Transactional
    public BookingResponse create(BookingRequest request) {
        int spots = request.numberOfSpots();

        // ---- Etape 1 : LECTURE et verification prealable (le "check") ----
        FitnessClassDto cls = fetchClass(request.classId());
        if (!"SCHEDULED".equals(cls.getStatus())) {
            throw new InvalidBookingOperationException("Le cours " + request.classId() + " n'est pas ouvert aux reservations");
        }
        if (cls.getCurrentParticipants() + spots > cls.getMaxParticipants()) {
            throw new NoSpotsAvailableForBookingException(cls.getId());
        }

        // ---- Etape 2 : reservation des places (le "use", re-verifie cote class-service) ----
        FitnessClassDto reserved = increment(request.classId(), spots);

        try {
            // ---- Etape 3 : creation de la reservation (snapshot) ----
            BigDecimal price = reserved.getPrice();
            BigDecimal totalAmount = price.multiply(BigDecimal.valueOf(spots));
            LocalDateTime now = LocalDateTime.now();
            Booking booking = new Booking(
                    ReferenceGenerator.nextBookingReference(),
                    request.userId(), request.userEmail(), request.userName(),
                    request.classId(), reserved.getName(), reserved.getDateTime(), reserved.getInstructor(),
                    price, spots, totalAmount, now, BookingStatus.PENDING_PAYMENT,
                    now.plusHours(1),                    // paymentDeadline = +1h
                    reserved.getDateTime().minusHours(24) // cancellationDeadline = classDate - 24h
            );
            Booking saved = repository.save(booking);

            // ---- Etape 4 : notification de reservation (best effort) ----
            sendNotificationQuietly(saved, "BOOKING_CONFIRMATION", "Reservation en attente de paiement",
                    "Bonjour " + request.userName() + ", votre reservation pour '" + saved.getClassName()
                            + "' est en attente de paiement. Payez avant " + saved.getPaymentDeadline() + ".");

            // ---- Etape 5 : retour au client ----
            return BookingMapper.toResponse(saved);
        } catch (RuntimeException ex) {
            // COMPENSATION : la reservation n'a pas pu etre persistee -> on libere les places.
            decrementQuietly(request.classId(), spots);
            throw ex;
        }
    }

    // =========================================================================
    // Cas 3 : confirmation apres paiement
    // =========================================================================
    @Transactional
    public BookingResponse confirm(Long id, ConfirmPaymentRequest request) {
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new InvalidBookingOperationException("Seule une reservation en attente de paiement peut etre confirmee");
        }
        if (booking.getPaymentDeadline().isBefore(LocalDateTime.now())) {
            throw new PaymentExpiredException(id);
        }

        PaymentDto payment = processPayment(booking, request);
        if ("SUCCESS".equals(payment.getStatus())) {
            booking.setStatus(BookingStatus.CONFIRMED);
            repository.save(booking);
            sendNotificationQuietly(booking, "PAYMENT_CONFIRMATION", "Paiement confirme",
                    "Votre paiement de " + booking.getTotalAmount() + " EUR pour '" + booking.getClassName()
                            + "' a ete accepte. A bientot !");
        }
        // Si le paiement ECHOUE (simulation >= 100 EUR), la reservation reste
        // PENDING_PAYMENT afin de laisser la possibilite de retenter.
        return BookingMapper.toResponse(booking);
    }

    // =========================================================================
    // Cas 4 : annulation (dans les delais)
    // =========================================================================
    @Transactional
    public BookingResponse cancel(Long id) {
        Booking booking = findBooking(id);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidBookingOperationException("La reservation est deja annulee");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new InvalidBookingOperationException("Impossible d'annuler une reservation terminee");
        }
        if (booking.getStatus() == BookingStatus.NO_SHOW) {
            throw new InvalidBookingOperationException("Impossible d'annuler une reservation marquee no-show");
        }
        if (booking.getCancellationDeadline().isBefore(LocalDateTime.now())) {
            throw new InvalidBookingOperationException("Annulation non autorisee : delai de 24 h avant le cours depasse");
        }

        boolean wasConfirmed = booking.getStatus() == BookingStatus.CONFIRMED;

        // ---- Remboursement (si paiement effectue) ----
        if (wasConfirmed) {
            refundQuietly(booking);
        }
        // ---- Liberation des places ----
        decrement(booking.getClassId(), booking.getNumberOfSpots());

        booking.setStatus(BookingStatus.CANCELLED);
        repository.save(booking);
        sendNotificationQuietly(booking, "BOOKING_CANCELLED", "Reservation annulee",
                "Votre reservation pour '" + booking.getClassName() + "' a ete annulee.");

        return BookingMapper.toResponse(booking);
    }

    @Transactional
    public BookingResponse complete(Long id) {
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new InvalidBookingOperationException("Seule une reservation confirmee peut etre marquee terminee");
        }
        booking.setStatus(BookingStatus.COMPLETED);
        repository.save(booking);
        return BookingMapper.toResponse(booking);
    }

    // =========================================================================
    // Scheduler : expiration des paiements & rappel des cours
    // =========================================================================

    /**
     * Annule automatiquement les reservations PENDING_PAYMENT dont le delai de
     * paiement est depasse : libere les places puis notifie.
     */
    @Transactional
    public void expirePendingPayments() {
        List<Booking> expired = repository.findByStatusAndPaymentDeadlineBefore(
                BookingStatus.PENDING_PAYMENT, LocalDateTime.now());
        for (Booking booking : expired) {
            try {
                decrement(booking.getClassId(), booking.getNumberOfSpots());
                booking.setStatus(BookingStatus.CANCELLED);
                repository.save(booking);
                sendNotificationQuietly(booking, "BOOKING_CANCELLED", "Paiement expire",
                        "Votre reservation pour '" + booking.getClassName() + "' a ete annulee faute de paiement.");
            } catch (RuntimeException ex) {
                // On n'interrompt pas le traitement des autres reservations.
            }
        }
    }

    /**
     * Envoie un rappel aux reservations CONFIRMED dont le cours a lieu dans ~24h.
     */
    @Transactional
    public void sendReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> upcoming = repository.findByStatusAndClassDateBetween(
                BookingStatus.CONFIRMED, now.plusHours(23), now.plusHours(25));
        for (Booking booking : upcoming) {
            sendNotificationQuietly(booking, "BOOKING_REMINDER", "Rappel de cours",
                    "Votre cours '" + booking.getClassName() + "' a lieu dans moins de 24 h (" + booking.getClassDate() + ").");
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Booking findBooking(Long id) {
        return repository.findById(id).orElseThrow(() -> new BookingNotFoundException(id));
    }

    private FitnessClassDto fetchClass(Long classId) {
        try {
            return classClient.getById(classId);
        } catch (FeignException.NotFound ex) {
            throw new InvalidBookingOperationException("Le cours " + classId + " n'existe pas");
        } catch (FeignException ex) {
            log.error("Feign [] echec getClass [{}] status={} url={} msg={}",
                    classClient.getClass().getSimpleName(), classId, ex.status(),
                    ex.request() != null ? ex.request().url() : "?",
                    ex.getMessage());
            throw new BookingServiceUnavailableException("class-service indisponible");
        }
    }

    private FitnessClassDto increment(Long classId, int spots) {
        try {
            return classClient.increment(classId, spots);
        } catch (FeignException.Conflict ex) {
            throw new NoSpotsAvailableForBookingException(classId);
        } catch (FeignException ex) {
            log.error("Feign [] echec increment [{}] spots={} status={} url={} msg={}",
                    classClient.getClass().getSimpleName(), classId, spots, ex.status(),
                    ex.request() != null ? ex.request().url() : "?",
                    ex.getMessage());
            log.error("BP diagnose increment", ex);
            throw new BookingServiceUnavailableException("class-service indisponible");
        }
    }

    private void decrement(Long classId, int spots) {
        try {
            classClient.decrement(classId, spots);
        } catch (FeignException.Conflict ex) {
            throw new InvalidBookingOperationException("Impossible de liberer les places du cours " + classId);
        } catch (FeignException ex) {
            throw new BookingServiceUnavailableException("class-service indisponible");
        }
    }

    private void decrementQuietly(Long classId, int spots) {
        try {
            classClient.decrement(classId, spots);
        } catch (RuntimeException ignored) {
            // compensation best effort (incl. fallback du circuit breaker)
        }
    }

    private PaymentDto processPayment(Booking booking, ConfirmPaymentRequest request) {
        try {
            return paymentClient.process(new PaymentRequestDto(
                    booking.getId(),
                    booking.getBookingReference(),
                    booking.getUserId(),
                    booking.getTotalAmount(),
                    request.paymentMethod(),
                    request.cardLastFour(),
                    request.transactionId()));
        } catch (FeignException ex) {
            throw new BookingServiceUnavailableException("payment-service indisponible");
        }
    }

    private void refundQuietly(Booking booking) {
        try {
            PaymentDto payment = paymentClient.getByBooking(booking.getId());
            if (payment.getId() != null) {
                paymentClient.refund(payment.getId());
            }
        } catch (RuntimeException ignored) {
            // remboursement best effort (incl. fallback du circuit breaker)
        }
    }

    private void sendNotificationQuietly(Booking booking, String type, String subject, String content) {
        try {
            notificationClient.send(new NotificationRequestDto(
                    booking.getUserId(), booking.getUserEmail(), type, subject, content));
        } catch (RuntimeException ignored) {
            // notification best effort (incl. fallback du circuit breaker)
        }
    }
}
