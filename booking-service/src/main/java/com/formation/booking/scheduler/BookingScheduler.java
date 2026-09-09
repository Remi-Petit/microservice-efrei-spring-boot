package com.formation.booking.scheduler;

import com.formation.booking.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Taches planifiees du booking-service :
 * <ul>
 *   <li>toutes les 5 minutes : expiration des paiements en attente ;</li>
 *   <li>toutes les 5 minutes : rappel des cours a venir (J-24h).</li>
 * </ul>
 */
@Component
public class BookingScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookingScheduler.class);

    private final BookingService bookingService;

    public BookingScheduler(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(fixedRate = 300_000)
    public void expirePendingPayments() {
        log.info("Scheduler : expiration des paiements en attente...");
        bookingService.expirePendingPayments();
    }

    @Scheduled(fixedRate = 300_000)
    public void sendReminders() {
        log.info("Scheduler : envoi des rappels de cours (J-24h)...");
        bookingService.sendReminders();
    }
}
