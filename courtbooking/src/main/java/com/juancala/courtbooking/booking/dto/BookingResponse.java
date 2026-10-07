package com.juancala.courtbooking.booking.dto;

import com.juancala.courtbooking.booking.Booking;
import com.juancala.courtbooking.booking.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public record BookingResponse(
        Long id,
        Long courtId,
        String courtName,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal price,
        BookingStatus status
) {

    /** Convierte los instantes guardados a la hora local del club. */
    public static BookingResponse from(Booking booking, ZoneId zone) {
        ZonedDateTime start = booking.getStartTime().atZone(zone);
        ZonedDateTime end = booking.getEndTime().atZone(zone);
        return new BookingResponse(
                booking.getId(),
                booking.getCourt().getId(),
                booking.getCourt().getName(),
                start.toLocalDate(),
                start.toLocalTime(),
                end.toLocalTime(),
                booking.getPrice(),
                booking.getStatus());
    }
}
