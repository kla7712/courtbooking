package com.juancala.courtbooking.court;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "courts")
public class Court {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Surface surface;

    @Column(nullable = false)
    private boolean indoor;

    @Column(name = "has_lighting", nullable = false)
    private boolean hasLighting;

    @Column(nullable = false)
    private boolean active = true;

    protected Court() {
        // requerido por JPA
    }

    public Court(String name, Surface surface, boolean indoor, boolean hasLighting) {
        this.name = name;
        this.surface = surface;
        this.indoor = indoor;
        this.hasLighting = hasLighting;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Surface getSurface() { return surface; }
    public boolean isIndoor() { return indoor; }
    public boolean isHasLighting() { return hasLighting; }
    public boolean isActive() { return active; }

    public void setName(String name) { this.name = name; }
    public void setSurface(Surface surface) { this.surface = surface; }
    public void setIndoor(boolean indoor) { this.indoor = indoor; }
    public void setHasLighting(boolean hasLighting) { this.hasLighting = hasLighting; }
    public void setActive(boolean active) { this.active = active; }
}
