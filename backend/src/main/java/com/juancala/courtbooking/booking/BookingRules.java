package com.juancala.courtbooking.booking;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Reglas de negocio configurables de las reservas (se leen de application.yml). */
@Component
public class BookingRules {

    public static final List<Integer> ALLOWED_DURATIONS = List.of(60, 90);
    public static final int SLOT_STEP_MINUTES = 30;

    private final ZoneId zone;
    private final LocalTime openingTime;
    private final LocalTime closingTime;
    private final int maxDaysInAdvance;
    private final int maxActiveBookings;
    private final int cancellationHours;
    private final LocalTime lightingFrom;
    private final BigDecimal lightingSupplementPerHour;

    public BookingRules(@Value("${app.booking.zone}") String zone,
                        @Value("${app.booking.opening-time}") String openingTime,
                        @Value("${app.booking.closing-time}") String closingTime,
                        @Value("${app.booking.max-days-in-advance}") int maxDaysInAdvance,
                        @Value("${app.booking.max-active-bookings}") int maxActiveBookings,
                        @Value("${app.booking.cancellation-hours}") int cancellationHours,
                        @Value("${app.booking.lighting-from}") String lightingFrom,
                        @Value("${app.booking.lighting-supplement-per-hour}") String lightingSupplementPerHour) {
        this.zone = ZoneId.of(zone);
        this.openingTime = LocalTime.parse(openingTime);
        this.closingTime = LocalTime.parse(closingTime);
        this.maxDaysInAdvance = maxDaysInAdvance;
        this.maxActiveBookings = maxActiveBookings;
        this.cancellationHours = cancellationHours;
        this.lightingFrom = LocalTime.parse(lightingFrom);
        this.lightingSupplementPerHour = new BigDecimal(lightingSupplementPerHour);
        if (!this.openingTime.isBefore(this.closingTime)) {
            throw new IllegalStateException("app.booking.opening-time debe ser anterior a closing-time");
        }
    }

    public ZoneId getZone() { return zone; }
    public LocalTime getOpeningTime() { return openingTime; }
    public LocalTime getClosingTime() { return closingTime; }
    public int getMaxDaysInAdvance() { return maxDaysInAdvance; }
    public int getMaxActiveBookings() { return maxActiveBookings; }
    public int getCancellationHours() { return cancellationHours; }

    /** Hora a partir de la cual hace falta luz artificial. */
    public LocalTime getLightingFrom() { return lightingFrom; }
    public BigDecimal getLightingSupplementPerHour() { return lightingSupplementPerHour; }
}
