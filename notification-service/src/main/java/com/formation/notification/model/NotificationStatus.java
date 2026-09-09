package com.formation.notification.model;

/**
 * Etat d'une notification (simulation d'envoi d'email/SMS).
 */
public enum NotificationStatus {
    PENDING,
    SENT,
    FAILED
}
