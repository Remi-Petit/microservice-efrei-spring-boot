package com.formation.classsvc.exception;

/**
 * Levee lors d'une operation incohérente sur le compteur de participants
 * (par exemple liberer plus de places qu'il n'y en a de reserves).
 */
public class InvalidSpotsOperationException extends RuntimeException {

    public InvalidSpotsOperationException(String message) {
        super(message);
    }
}
