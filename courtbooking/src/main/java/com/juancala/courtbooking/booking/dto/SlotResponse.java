package com.juancala.courtbooking.booking.dto;

import java.time.LocalTime;
import java.util.List;

/** Una hora de inicio posible y las duraciones (en minutos) que caben desde ella. */
public record SlotResponse(
        LocalTime startTime,
        List<Integer> availableDurations
) {
}
