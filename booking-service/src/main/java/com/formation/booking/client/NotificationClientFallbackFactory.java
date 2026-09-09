package com.formation.booking.client;

import com.formation.booking.dto.NotificationDto;
import com.formation.booking.dto.NotificationRequestDto;
import org.springframework.cloud.openfeign.FallbackFactory;

/**
 * Fallback du client notification-service (Circuit Breaker resilience4j).
 * <p>
 * La notification est <b>best-effort</b> : elle ne doit jamais bloquer la saga.
 * En cas de panne du notification-service, on renvoie simplement une notification
 * marquee {@code FAILED}, sans lever d'exception, pour laisser la reservation se terminer.
 */
public class NotificationClientFallbackFactory implements FallbackFactory<NotificationClient> {

    @Override
    public NotificationClient create(Throwable cause) {
        return new NotificationClient() {
            @Override
            public NotificationDto send(NotificationRequestDto request) {
                NotificationDto dto = new NotificationDto();
                dto.setStatus("FAILED");
                return dto;
            }
        };
    }
}
