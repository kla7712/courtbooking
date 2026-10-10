package com.juancala.courtbooking.booking.dto;

import java.math.BigDecimal;

/** Una duración reservable desde una hora de inicio, con su precio. */
public record SlotOption(
        int durationMinutes,
        BigDecimal price
) {
}
