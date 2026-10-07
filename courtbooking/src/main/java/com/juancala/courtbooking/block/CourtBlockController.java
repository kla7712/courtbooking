package com.juancala.courtbooking.block;

import com.juancala.courtbooking.block.dto.BlockResponse;
import com.juancala.courtbooking.block.dto.CreateBlockRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cuelga de /api/courts, así que hereda sus permisos:
 * consultar es público y crear o borrar es solo para administradores.
 */
@RestController
@RequestMapping("/api/courts/{courtId}/blocks")
public class CourtBlockController {

    private final CourtBlockService courtBlockService;

    public CourtBlockController(CourtBlockService courtBlockService) {
        this.courtBlockService = courtBlockService;
    }

    @GetMapping
    public List<BlockResponse> findByDate(
            @PathVariable Long courtId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return courtBlockService.findByCourtAndDate(courtId, date);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BlockResponse create(@PathVariable Long courtId, @Valid @RequestBody CreateBlockRequest request) {
        return courtBlockService.create(courtId, request);
    }

    @DeleteMapping("/{blockId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long courtId, @PathVariable Long blockId) {
        courtBlockService.delete(courtId, blockId);
    }
}
