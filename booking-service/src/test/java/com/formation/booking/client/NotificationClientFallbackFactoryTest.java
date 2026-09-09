package com.formation.booking.client;

import com.formation.booking.dto.NotificationDto;
import com.formation.booking.dto.NotificationRequestDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationClientFallbackFactoryTest {

    private final NotificationClientFallbackFactory factory = new NotificationClientFallbackFactory();

    @Test
    void send_retourneUneNotificationEnEchec() {
        NotificationClient client = factory.create(new RuntimeException("notification down"));

        NotificationDto dto = client.send(new NotificationRequestDto(
                7L, "john@example.com", "BOOKING_CONFIRMATION", "Sujet", "Contenu"));

        assertThat(dto.getStatus()).isEqualTo("FAILED");
    }
}
