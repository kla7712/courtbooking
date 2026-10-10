package com.juancala.courtbooking.booking.dto;

import com.juancala.courtbooking.booking.Booking;
import com.juancala.courtbooking.booking.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Una reserva vista por el administrador: incluye quién la hizo. */
public record AdminBookingResponse(
        Long id,
        Long courtId,
        String courtName,
        String userName,
        String userEmail,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal price,
        BookingStatus status
) {

    public static AdminBookingResponse from(Booking booking, ZoneId zone) {
        ZonedDateTime start = booking.getStartTime().atZone(zone);
        ZonedDateTime end = booking.getEndTime().atZone(zone);
        return new AdminBookingResponse(
                booking.getId(),
                booking.getCourt().getId(),
                booking.getCourt().getName(),
                booking.getUser().getName(),
                booking.getUser().getEmail(),
                start.toLocalDate(),
                start.toLocalTime(),
                end.toLocalTime(),
                booking.getPrice(),
                booking.getStatus());
    }
}
