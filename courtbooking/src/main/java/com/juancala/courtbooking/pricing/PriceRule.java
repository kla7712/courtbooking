package com.juancala.courtbooking.pricing;

import com.juancala.courtbooking.court.Surface;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "price_rules")
public class PriceRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Surface surface;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", nullable = false, length = 10)
    private DayType dayType;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "price_per_hour", nullable = false, precision = 6, scale = 2)
    private BigDecimal pricePerHour;

    protected PriceRule() {
        // requerido por JPA
    }

    /** La franja es [inicio, fin): incluye la hora de inicio y excluye la de fin. */
    public boolean covers(LocalTime time) {
        return !time.isBefore(startTime) && time.isBefore(endTime);
    }

    public Long getId() { return id; }
    public Surface getSurface() { return surface; }
    public DayType getDayType() { return dayType; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public BigDecimal getPricePerHour() { return pricePerHour; }

    public void setPricePerHour(BigDecimal pricePerHour) { this.pricePerHour = pricePerHour; }
}
