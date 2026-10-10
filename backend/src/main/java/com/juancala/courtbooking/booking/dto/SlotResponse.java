package com.juancala.courtbooking.booking.dto;

import java.time.LocalTime;
import java.util.List;

/** Una hora de inicio posible y las duraciones que caben desde ella, cada una con su precio. */
public record SlotResponse(
        LocalTime startTime,
        List<SlotOption> options
) {
}
