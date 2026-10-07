package com.juancala.courtbooking.pricing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdatePriceRequest(
        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        @Digits(integer = 4, fraction = 2, message = "El precio admite como máximo 4 enteros y 2 decimales")
        BigDecimal pricePerHour
) {
}
