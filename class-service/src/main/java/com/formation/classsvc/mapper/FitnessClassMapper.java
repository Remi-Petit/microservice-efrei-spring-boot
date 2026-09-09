package com.formation.classsvc.mapper;

import com.formation.classsvc.dto.FitnessClassRequest;
import com.formation.classsvc.dto.FitnessClassResponse;
import com.formation.classsvc.model.ClassStatus;
import com.formation.classsvc.model.FitnessClass;

public final class FitnessClassMapper {

    private FitnessClassMapper() {
    }

    public static FitnessClassResponse toResponse(FitnessClass c) {
        if (c == null) {
            return null;
        }
        return new FitnessClassResponse(
                c.getId(),
                c.getName(),
                c.getDescription(),
                c.getInstructor(),
                c.getGymLocation(),
                c.getCategory(),
                c.getLevel(),
                c.getDurationMinutes(),
                c.getMaxParticipants(),
                c.getCurrentParticipants(),
                c.getPrice(),
                c.getDateTime(),
                c.getStatus(),
                c.getVersion()
        );
    }

    /**
     * A la creation, aucun participant n'est encore inscrit : currentParticipants = 0
     * et le cours est SCHEDULED.
     */
    public static FitnessClass toEntity(FitnessClassRequest request) {
        return new FitnessClass(
                request.name(),
                request.description(),
                request.instructor(),
                request.gymLocation(),
                request.category(),
                request.level(),
                request.durationMinutes(),
                request.maxParticipants(),
                0,
                request.price(),
                request.dateTime(),
                ClassStatus.SCHEDULED
        );
    }
}
