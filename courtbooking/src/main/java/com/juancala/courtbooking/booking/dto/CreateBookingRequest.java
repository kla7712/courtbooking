package com.juancala.courtbooking.booking.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

/** Fecha y hora van en la hora local del club, p. ej. "2026-10-08" y "18:00". */
public record CreateBookingRequest(
        @NotNull(message = "La pista es obligatoria")
        Long courtId,

        @NotNull(message = "La fecha es obligatoria")
        LocalDate date,

        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime startTime,

        @NotNull(message = "La duración es obligatoria")
        Integer durationMinutes
) {
}
