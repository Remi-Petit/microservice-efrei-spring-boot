package com.formation.classsvc.exception;

/**
 * Levee lorsque le nombre de places demandees depasse la capacite restante du cours
 * (y compris en cas de conflit detectee par le verrouillage optimiste).
 */
public class NoSpotsAvailableException extends RuntimeException {

    public NoSpotsAvailableException(String message) {
        super(message);
    }
}
