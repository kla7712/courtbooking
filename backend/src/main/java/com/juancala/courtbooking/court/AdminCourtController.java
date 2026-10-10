package com.juancala.courtbooking.court;

import com.juancala.courtbooking.court.dto.CourtResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Consultas de pistas para el panel de administración (solo admin, ver SecurityConfig). */
@RestController
@RequestMapping("/api/admin/courts")
public class AdminCourtController {

    private final CourtService courtService;

    public AdminCourtController(CourtService courtService) {
        this.courtService = courtService;
    }

    /** Todas las pistas, incluidas las desactivadas. */
    @GetMapping
    public List<CourtResponse> findAll() {
        return courtService.findAllIncludingInactive();
    }
}
