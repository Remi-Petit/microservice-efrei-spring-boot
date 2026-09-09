package com.formation.notification.dto;

import com.formation.notification.model.NotificationStatus;
import com.formation.notification.model.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        Long userId,
        String email,
        NotificationType type,
        String subject,
        String content,
        LocalDateTime sentDate,
        NotificationStatus status
) {
}
