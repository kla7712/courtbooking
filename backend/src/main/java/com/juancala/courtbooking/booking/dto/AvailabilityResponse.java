package com.juancala.courtbooking.booking.dto;

import java.time.LocalDate;
import java.util.List;

public record AvailabilityResponse(
        Long courtId,
        String courtName,
        LocalDate date,
        List<SlotResponse> slots
) {
}
