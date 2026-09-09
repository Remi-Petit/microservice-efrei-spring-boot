package com.formation.notification;

import com.formation.notification.dto.NotificationRequest;
import com.formation.notification.dto.NotificationResponse;
import com.formation.notification.exception.NotificationNotRetryableException;
import com.formation.notification.model.Notification;
import com.formation.notification.model.NotificationStatus;
import com.formation.notification.model.NotificationType;
import com.formation.notification.repository.NotificationRepository;
import com.formation.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    @InjectMocks
    private NotificationService service;

    private NotificationRequest request(String email) {
        return new NotificationRequest(1L, email, NotificationType.BOOKING_CONFIRMATION,
                "Sujet", "Contenu de la notification");
    }

    private Notification notification(String email, NotificationStatus status) {
        return new Notification(1L, email, NotificationType.BOOKING_CONFIRMATION,
                "Sujet", "Contenu", LocalDateTime.now(), status);
    }

    @Test
    void send_emailValide_marqueEnvoye() {
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse result = service.send(request("john@example.com"));

        assertThat(result.status()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void send_emailInvalide_marqueEchec() {
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse result = service.send(request("sans-arobase"));

        assertThat(result.status()).isEqualTo(NotificationStatus.FAILED);
    }

    @Test
    void retry_notificationEnEchec_reussit() {
        Notification n = notification("ok@example.com", NotificationStatus.FAILED);
        when(repository.findById(1L)).thenReturn(Optional.of(n));
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse result = service.retry(1L);

        assertThat(result.status()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void retry_notificationDejaEnvoyee_leveException() {
        when(repository.findById(1L)).thenReturn(Optional.of(notification("ok@example.com", NotificationStatus.SENT)));

        assertThatThrownBy(() -> service.retry(1L))
                .isInstanceOf(NotificationNotRetryableException.class);
    }

    @Test
    void getPending_retourneLesNotificationsEnAttente() {
        when(repository.findByStatus(NotificationStatus.PENDING))
                .thenReturn(List.of(notification("a@b.com", NotificationStatus.PENDING)));

        List<NotificationResponse> pending = service.getPending();

        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).status()).isEqualTo(NotificationStatus.PENDING);
    }
}
