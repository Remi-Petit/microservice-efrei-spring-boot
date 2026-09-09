package com.formation.classsvc.exception;

public class FitnessClassNotFoundException extends RuntimeException {

    public FitnessClassNotFoundException(Long id) {
        super("Cours introuvable : id " + id);
    }
}
