package com.formation.booking.mapper;

import com.formation.booking.dto.BookingResponse;
import com.formation.booking.model.Booking;

public final class BookingMapper {

    private BookingMapper() {
    }

    public static BookingResponse toResponse(Booking b) {
        if (b == null) {
            return null;
        }
        return new BookingResponse(
                b.getId(),
                b.getBookingReference(),
                b.getUserId(),
                b.getUserEmail(),
                b.getUserName(),
                b.getClassId(),
                b.getClassName(),
                b.getClassDate(),
                b.getInstructor(),
                b.getPrice(),
                b.getNumberOfSpots(),
                b.getTotalAmount(),
                b.getBookingDate(),
                b.getStatus(),
                b.getPaymentDeadline(),
                b.getCancellationDeadline()
        );
    }
}
