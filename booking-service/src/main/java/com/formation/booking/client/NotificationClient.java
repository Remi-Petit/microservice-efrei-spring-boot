package com.formation.booking.client;

import com.formation.booking.dto.NotificationDto;
import com.formation.booking.dto.NotificationRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Contrat Feign vers notification-service : envoi d'une notification.
 */
@FeignClient(name = "notification-service", fallbackFactory = NotificationClientFallbackFactory.class)
public interface NotificationClient {

    @PostMapping("/api/notifications")
    NotificationDto send(@RequestBody NotificationRequestDto request);
}
