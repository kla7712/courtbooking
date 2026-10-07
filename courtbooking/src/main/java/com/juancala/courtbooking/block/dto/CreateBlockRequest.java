package com.juancala.courtbooking.block.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/** Fecha y horas van en la hora local del club. */
public record CreateBlockRequest(
        @NotNull(message = "La fecha es obligatoria")
        LocalDate date,

        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime startTime,

        @NotNull(message = "La hora de fin es obligatoria")
        LocalTime endTime,

        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 200, message = "El motivo no puede superar los 200 caracteres")
        String reason
) {
}
