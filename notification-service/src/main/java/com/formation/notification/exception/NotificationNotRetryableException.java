package com.formation.notification.exception;

/**
 * Levee lorsqu'on tente de reessayer l'envoi d'une notification qui n'est pas
 * en echec (deja envoyee ou deja en attente).
 */
public class NotificationNotRetryableException extends RuntimeException {

    public NotificationNotRetryableException(String message) {
        super(message);
    }
}
