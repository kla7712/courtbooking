package com.juancala.courtbooking.court.dto;

import com.juancala.courtbooking.court.Surface;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CourtRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 50, message = "El nombre no puede superar los 50 caracteres")
        String name,

        @NotNull(message = "La superficie es obligatoria")
        Surface surface,

        boolean indoor,

        boolean hasLighting
) {
}
