package com.formation.booking.dto;

/**
 * Corps envoye a notification-service ({@code POST /api/notifications}).
 */
public record NotificationRequestDto(
        Long userId,
        String email,
        String type,
        String subject,
        String content
) {
}
