package com.formation.booking;

import com.formation.booking.client.ClassClient;
import com.formation.booking.client.NotificationClient;
import com.formation.booking.client.PaymentClient;
import com.formation.booking.dto.BookingRequest;
import com.formation.booking.dto.BookingResponse;
import com.formation.booking.dto.ConfirmPaymentRequest;
import com.formation.booking.dto.FitnessClassDto;
import com.formation.booking.dto.NotificationRequestDto;
import com.formation.booking.dto.PaymentDto;
import com.formation.booking.exception.InvalidBookingOperationException;
import com.formation.booking.exception.NoSpotsAvailableForBookingException;
import com.formation.booking.exception.PaymentExpiredException;
import com.formation.booking.model.Booking;
import com.formation.booking.model.BookingStatus;
import com.formation.booking.repository.BookingRepository;
import com.formation.booking.service.BookingService;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository repository;
    @Mock
    private ClassClient classClient;
    @Mock
    private PaymentClient paymentClient;
    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private BookingService service;

    private FitnessClassDto cls(int max, int current, String status) {
        FitnessClassDto d = new FitnessClassDto();
        d.setId(1L);
        d.setName("Yoga du matin");
        d.setInstructor("Marie");
        d.setPrice(new BigDecimal("20.00"));
        d.setDateTime(LocalDateTime.now().plusDays(7));
        d.setMaxParticipants(max);
        d.setCurrentParticipants(current);
        d.setStatus(status);
        return d;
    }

    private BookingRequest req() {
        return new BookingRequest(7L, "john@example.com", "John Doe", 1L, 2);
    }

    private Booking booking(BookingStatus status, LocalDateTime paymentDeadline, LocalDateTime cancelDeadline) {
        Booking b = new Booking("BK-12345", 7L, "john@example.com", "John Doe", 1L,
                "Yoga du matin", LocalDateTime.now().plusDays(7), "Marie",
                new BigDecimal("20.00"), 2, new BigDecimal("40.00"), LocalDateTime.now(), status,
                paymentDeadline, cancelDeadline);
        b.setId(1L);
        return b;
    }

    private FeignException.NotFound notFound() {
        Request request = Request.create(Request.HttpMethod.GET, "/api/classes/999",
                new HashMap<>(), (byte[]) null, null);
        return new FeignException.NotFound("Not Found", request, null, new HashMap<>());
    }

    private FeignException.Conflict conflict() {
        Request request = Request.create(Request.HttpMethod.PATCH, "/api/classes/1/increment",
                new HashMap<>(), (byte[]) null, null);
        return new FeignException.Conflict("Conflict", request, null, new HashMap<>());
    }

    private PaymentDto payment(String status) {
        PaymentDto p = new PaymentDto();
        p.setId(1L);
        p.setStatus(status);
        return p;
    }

    @Test
    void create_quandPlacesDisponibles_creeReservationPendingPayment() {
        when(classClient.getById(1L)).thenReturn(cls(10, 5, "SCHEDULED"));
        when(classClient.increment(1L, 2)).thenReturn(cls(10, 7, "SCHEDULED"));
        when(repository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });

        BookingResponse result = service.create(req());

        verify(classClient).increment(1L, 2);
        assertThat(result.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("40.00"));
        assertThat(result.bookingReference()).startsWith("BK-");
        assertThat(result.paymentDeadline()).isAfter(LocalDateTime.now());
    }

    @Test
    void create_plusDePlaces_leveNoSpotsAvailableException() {
        // TOCTOU : la lecture (etape 1) voit encore 8/10 places, mais entre la
        // verification et la reservation (etape 2), un concurrent epuise les places.
        when(classClient.getById(1L)).thenReturn(cls(10, 8, "SCHEDULED"));
        when(classClient.increment(1L, 2)).thenThrow(conflict());

        assertThatThrownBy(() -> service.create(req()))
                .isInstanceOf(NoSpotsAvailableForBookingException.class);

        // Aucune reservation ne doit avoir ete creee.
        verify(repository, never()).save(any(Booking.class));
    }

    @Test
    void create_coursInexistant_leveInvalidBookingOperation() {
        when(classClient.getById(999L)).thenThrow(notFound());

        assertThatThrownBy(() -> service.create(new BookingRequest(7L, "john@example.com", "John Doe", 999L, 1)))
                .isInstanceOf(InvalidBookingOperationException.class);
    }

    @Test
    void confirm_paiementReussi_passeAConfirme() {
        when(repository.findById(1L)).thenReturn(Optional.of(
                booking(BookingStatus.PENDING_PAYMENT, LocalDateTime.now().plusMinutes(30),
                        LocalDateTime.now().plusDays(6))));
        when(paymentClient.process(any())).thenReturn(payment("SUCCESS"));

        BookingResponse result = service.confirm(1L, new ConfirmPaymentRequest("CREDIT_CARD", "1234", "txn_1"));

        assertThat(result.status()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void confirm_paiementEchoue_resteEnAttente() {
        when(repository.findById(1L)).thenReturn(Optional.of(
                booking(BookingStatus.PENDING_PAYMENT, LocalDateTime.now().plusMinutes(30),
                        LocalDateTime.now().plusDays(6))));
        when(paymentClient.process(any())).thenReturn(payment("FAILED"));

        BookingResponse result = service.confirm(1L, new ConfirmPaymentRequest("CREDIT_CARD", "1234", "txn_2"));

        assertThat(result.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    void confirm_delaiDepasse_levePaymentExpiredException() {
        when(repository.findById(1L)).thenReturn(Optional.of(
                booking(BookingStatus.PENDING_PAYMENT, LocalDateTime.now().minusMinutes(5),
                        LocalDateTime.now().plusDays(6))));

        assertThatThrownBy(() -> service.confirm(1L, new ConfirmPaymentRequest("CREDIT_CARD", "1234", "txn_3")))
                .isInstanceOf(PaymentExpiredException.class);
    }

    @Test
    void confirm_statutNonEnAttente_leveInvalidBookingOperation() {
        when(repository.findById(1L)).thenReturn(Optional.of(
                booking(BookingStatus.CONFIRMED, LocalDateTime.now().plusMinutes(30),
                        LocalDateTime.now().plusDays(6))));

        assertThatThrownBy(() -> service.confirm(1L, new ConfirmPaymentRequest("CREDIT_CARD", "1234", "txn_4")))
                .isInstanceOf(InvalidBookingOperationException.class);
    }

    @Test
    void cancel_dansLesDelais_libereLesPlacesEtRembourse() {
        when(repository.findById(1L)).thenReturn(Optional.of(
                booking(BookingStatus.CONFIRMED, LocalDateTime.now().plusMinutes(30),
                        LocalDateTime.now().plusDays(6))));
        when(paymentClient.getByBooking(1L)).thenReturn(payment("SUCCESS"));
        when(paymentClient.refund(1L)).thenReturn(payment("REFUNDED"));
        when(classClient.decrement(1L, 2)).thenReturn(cls(10, 5, "SCHEDULED"));
        when(repository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse result = service.cancel(1L);

        verify(classClient).decrement(1L, 2);
        verify(paymentClient).refund(1L);
        assertThat(result.status()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cancel_apresLeDelai_leveInvalidBookingOperation() {
        when(repository.findById(1L)).thenReturn(Optional.of(
                booking(BookingStatus.CONFIRMED, LocalDateTime.now().plusMinutes(30),
                        LocalDateTime.now().minusMinutes(1))));

        assertThatThrownBy(() -> service.cancel(1L))
                .isInstanceOf(InvalidBookingOperationException.class);

        verify(classClient, never()).decrement(any(Long.class), any(Integer.class));
    }

    @Test
    void complete_passeAComplet() {
        when(repository.findById(1L)).thenReturn(Optional.of(
                booking(BookingStatus.CONFIRMED, LocalDateTime.now().plusMinutes(30),
                        LocalDateTime.now().plusDays(6))));
        when(repository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse result = service.complete(1L);

        assertThat(result.status()).isEqualTo(BookingStatus.COMPLETED);
    }

    @Test
    void expirePendingPayments_annuleLesReservationsEnRetard() {
        Booking expired = booking(BookingStatus.PENDING_PAYMENT, LocalDateTime.now().minusMinutes(10),
                LocalDateTime.now().plusDays(6));
        when(repository.findByStatusAndPaymentDeadlineBefore(any(), any(LocalDateTime.class)))
                .thenReturn(List.of(expired));
        when(classClient.decrement(1L, 2)).thenReturn(cls(10, 5, "SCHEDULED"));
        when(repository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        service.expirePendingPayments();

        assertThat(expired.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void create_envoieNotificationDeConfirmation() {
        when(classClient.getById(1L)).thenReturn(cls(10, 5, "SCHEDULED"));
        when(classClient.increment(1L, 2)).thenReturn(cls(10, 7, "SCHEDULED"));
        when(repository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });

        service.create(req());

        ArgumentCaptor<NotificationRequestDto> captor = ArgumentCaptor.forClass(NotificationRequestDto.class);
        verify(notificationClient).send(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo("BOOKING_CONFIRMATION");
        assertThat(captor.getValue().userId()).isEqualTo(7L);
    }

    @Test
    void cancel_envoieNotificationDAnnulation() {
        when(repository.findById(1L)).thenReturn(Optional.of(
                booking(BookingStatus.CONFIRMED, LocalDateTime.now().plusMinutes(30),
                        LocalDateTime.now().plusDays(6))));
        when(paymentClient.getByBooking(1L)).thenReturn(payment("SUCCESS"));
        when(paymentClient.refund(1L)).thenReturn(payment("REFUNDED"));
        when(classClient.decrement(1L, 2)).thenReturn(cls(10, 5, "SCHEDULED"));
        when(repository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        service.cancel(1L);

        ArgumentCaptor<NotificationRequestDto> captor = ArgumentCaptor.forClass(NotificationRequestDto.class);
        verify(notificationClient).send(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo("BOOKING_CANCELLED");
    }

    @Test
    void sendReminders_envoieNotificationsPourCoursDans24h() {
        Booking upcoming = booking(BookingStatus.CONFIRMED, LocalDateTime.now().plusMinutes(30),
                LocalDateTime.now().plusDays(6));
        when(repository.findByStatusAndClassDateBetween(any(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(upcoming));

        service.sendReminders();

        ArgumentCaptor<NotificationRequestDto> captor = ArgumentCaptor.forClass(NotificationRequestDto.class);
        verify(notificationClient).send(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo("BOOKING_REMINDER");
    }
}
