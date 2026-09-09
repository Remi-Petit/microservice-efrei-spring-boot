package com.formation.classsvc.dto;

import com.formation.classsvc.model.Category;
import com.formation.classsvc.model.ClassStatus;
import com.formation.classsvc.model.Level;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FitnessClassResponse(
        Long id,
        String name,
        String description,
        String instructor,
        String gymLocation,
        Category category,
        Level level,
        Integer durationMinutes,
        Integer maxParticipants,
        Integer currentParticipants,
        BigDecimal price,
        LocalDateTime dateTime,
        ClassStatus status,
        Long version
) {
}
