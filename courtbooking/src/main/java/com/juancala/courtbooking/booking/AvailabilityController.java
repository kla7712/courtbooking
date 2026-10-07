package com.juancala.courtbooking.booking;

import com.juancala.courtbooking.booking.dto.AvailabilityResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AvailabilityController {

    private final BookingService bookingService;

    public AvailabilityController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /** Público: cualquiera puede consultar los huecos libres, p. ej. ?date=2026-10-08 */
    @GetMapping("/api/courts/{courtId}/availability")
    public AvailabilityResponse availability(
            @PathVariable Long courtId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return bookingService.getAvailability(courtId, date);
    }
}
