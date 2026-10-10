package com.juancala.courtbooking.court;

import com.juancala.courtbooking.court.dto.CourtRequest;
import com.juancala.courtbooking.court.dto.CourtResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courts")
public class CourtController {

    private final CourtService courtService;

    public CourtController(CourtService courtService) {
        this.courtService = courtService;
    }

    @GetMapping
    public List<CourtResponse> findAll(@RequestParam(required = false) Surface surface) {
        return courtService.findAll(surface);
    }

    @GetMapping("/{id}")
    public CourtResponse findById(@PathVariable Long id) {
        return courtService.findById(id);
    }

    @PostMapping
    public ResponseEntity<CourtResponse> create(@Valid @RequestBody CourtRequest request) {
        CourtResponse created = courtService.create(request);
        return ResponseEntity
                .created(URI.create("/api/courts/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public CourtResponse update(@PathVariable Long id, @Valid @RequestBody CourtRequest request) {
        return courtService.update(id, request);
    }

    /** Solo admin: reactiva una pista desactivada. */
    @PutMapping("/{id}/activate")
    public CourtResponse activate(@PathVariable Long id) {
        return courtService.activate(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        courtService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
