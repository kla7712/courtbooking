package com.juancala.courtbooking.booking;

import com.juancala.courtbooking.booking.dto.AdminBookingResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consultas de reservas para el panel de administración (solo admin, ver SecurityConfig). */
@RestController
@RequestMapping("/api/admin/bookings")
public class AdminBookingController {

    private final BookingService bookingService;

    public AdminBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /** Reservas de todos los socios en un día, p. ej. ?date=2026-10-12 */
    @GetMapping
    public List<AdminBookingResponse> findByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return bookingService.findByDate(date);
    }
}
