package com.formation.notification.mapper;

import com.formation.notification.dto.NotificationRequest;
import com.formation.notification.dto.NotificationResponse;
import com.formation.notification.model.Notification;
import com.formation.notification.model.NotificationStatus;

import java.time.LocalDateTime;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static NotificationResponse toResponse(Notification n) {
        if (n == null) {
            return null;
        }
        return new NotificationResponse(
                n.getId(),
                n.getUserId(),
                n.getEmail(),
                n.getType(),
                n.getSubject(),
                n.getContent(),
                n.getSentDate(),
                n.getStatus()
        );
    }

    /**
     * Construit une entite {@link Notification} en statut PENDING,
     * la date d'envoi etant fixee a maintenant.
     */
    public static Notification toEntity(NotificationRequest request) {
        return new Notification(
                request.userId(),
                request.email(),
                request.type(),
                request.subject(),
                request.content(),
                LocalDateTime.now(),
                NotificationStatus.PENDING
        );
    }
}
