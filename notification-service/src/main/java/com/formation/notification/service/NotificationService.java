package com.formation.notification.service;

import com.formation.notification.dto.NotificationRequest;
import com.formation.notification.dto.NotificationResponse;
import com.formation.notification.exception.NotificationNotFoundException;
import com.formation.notification.exception.NotificationNotRetryableException;
import com.formation.notification.mapper.NotificationMapper;
import com.formation.notification.model.Notification;
import com.formation.notification.model.NotificationStatus;
import com.formation.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    /**
     * Envoie une notification (appele par les autres services). Le statut est
     * determine par une <b>simulation d'envoi</b> : si un email valide est fourni
     * la notification est SENT, sinon elle est en echec (FAILED).
     */
    @Transactional
    public NotificationResponse send(NotificationRequest request) {
        Notification notification = NotificationMapper.toEntity(request);
        notification.setStatus(simulateSend(notification.getEmail()));
        notification.setSentDate(LocalDateTime.now());
        return NotificationMapper.toResponse(repository.save(notification));
    }

    public List<NotificationResponse> getByUserId(Long userId) {
        return repository.findByUserIdOrderBySentDateDesc(userId).stream()
                .map(NotificationMapper::toResponse)
                .toList();
    }

    /** Notifications encore en attente (pour le scheduler). */
    public List<NotificationResponse> getPending() {
        return repository.findByStatus(NotificationStatus.PENDING).stream()
                .map(NotificationMapper::toResponse)
                .toList();
    }

    /**
     * Reessaie l'envoi d'une notification en echec.
     */
    @Transactional
    public NotificationResponse retry(Long id) {
        Notification notification = repository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));
        if (notification.getStatus() != NotificationStatus.FAILED) {
            throw new NotificationNotRetryableException("Seule une notification en echec peut etre relancee");
        }
        notification.setStatus(NotificationStatus.PENDING);
        notification.setStatus(simulateSend(notification.getEmail()));
        notification.setSentDate(LocalDateTime.now());
        return NotificationMapper.toResponse(repository.save(notification));
    }

    private NotificationStatus simulateSend(String email) {
        return (email != null && email.contains("@")) ? NotificationStatus.SENT : NotificationStatus.FAILED;
    }
}
