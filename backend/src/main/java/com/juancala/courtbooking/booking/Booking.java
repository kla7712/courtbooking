package com.juancala.courtbooking.booking;

import com.juancala.courtbooking.court.Court;
import com.juancala.courtbooking.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    // Se guardan como instantes (UTC); la conversión a la hora local del club se hace en el servicio
    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    // Precio calculado al reservar; no cambia aunque después se modifiquen las tarifas
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal price;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Booking() {
        // requerido por JPA
    }

    public Booking(User user, Court court, Instant startTime, Instant endTime, BigDecimal price) {
        this.user = user;
        this.court = court;
        this.startTime = startTime;
        this.endTime = endTime;
        this.price = price;
        this.status = BookingStatus.CONFIRMED;
        this.createdAt = Instant.now();
    }

    public void cancel() {
        this.status = BookingStatus.CANCELLED;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Court getCourt() { return court; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public BookingStatus getStatus() { return status; }
    public BigDecimal getPrice() { return price; }
    public Instant getCreatedAt() { return createdAt; }
}
