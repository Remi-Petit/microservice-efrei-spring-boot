package com.formation.classsvc.exception;

/**
 * Levee lorsque l'on tente de modifier ou de reserver un cours qui n'est plus
 * ouvert aux inscriptions (deja annule ou deja termine).
 */
public class ClassNotModifiableException extends RuntimeException {

    public ClassNotModifiableException(String message) {
        super(message);
    }
}
