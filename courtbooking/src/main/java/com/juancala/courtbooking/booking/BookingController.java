package com.juancala.courtbooking.booking;

import com.juancala.courtbooking.booking.dto.BookingResponse;
import com.juancala.courtbooking.booking.dto.CreateBookingRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@AuthenticationPrincipal Jwt jwt,
                                  @Valid @RequestBody CreateBookingRequest request) {
        return bookingService.create(userId(jwt), request);
    }

    @GetMapping("/mine")
    public List<BookingResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return bookingService.findByUser(userId(jwt));
    }

    /** Cancela la reserva (no se borra: queda con estado CANCELLED). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        boolean isAdmin = "ADMIN".equals(jwt.getClaimAsString("role"));
        bookingService.cancel(id, userId(jwt), isAdmin);
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
