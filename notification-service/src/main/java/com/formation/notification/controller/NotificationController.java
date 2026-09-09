package com.formation.notification.controller;

import com.formation.notification.dto.NotificationRequest;
import com.formation.notification.dto.NotificationResponse;
import com.formation.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** Envoie une notification (appele par les autres services). */
    @PostMapping
    public ResponseEntity<NotificationResponse> send(@Valid @RequestBody NotificationRequest request) {
        NotificationResponse created = notificationService.send(request);
        return ResponseEntity.created(URI.create("/api/notifications/" + created.id())).body(created);
    }

    /** Historique des notifications d'un utilisateur. */
    @GetMapping("/user/{userId}")
    public List<NotificationResponse> getByUser(@PathVariable Long userId) {
        return notificationService.getByUserId(userId);
    }

    /** Notifications en attente (pour le scheduler). */
    @GetMapping("/pending")
    public List<NotificationResponse> getPending() {
        return notificationService.getPending();
    }

    /** Reessaie l'envoi d'une notification en echec. */
    @PatchMapping("/{id}/retry")
    public NotificationResponse retry(@PathVariable Long id) {
        return notificationService.retry(id);
    }
}
