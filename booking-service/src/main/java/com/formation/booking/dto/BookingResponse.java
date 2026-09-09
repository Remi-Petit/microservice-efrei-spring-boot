package com.formation.booking.dto;

import com.formation.booking.model.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        String bookingReference,
        Long userId,
        String userEmail,
        String userName,
        Long classId,
        String className,
        LocalDateTime classDate,
        String instructor,
        BigDecimal price,
        Integer numberOfSpots,
        BigDecimal totalAmount,
        LocalDateTime bookingDate,
        BookingStatus status,
        LocalDateTime paymentDeadline,
        LocalDateTime cancellationDeadline
) {
}
