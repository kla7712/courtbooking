package com.juancala.courtbooking.court;

import com.juancala.courtbooking.common.ConflictException;
import com.juancala.courtbooking.common.NotFoundException;
import com.juancala.courtbooking.court.dto.CourtRequest;
import com.juancala.courtbooking.court.dto.CourtResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CourtService {

    private final CourtRepository courtRepository;

    public CourtService(CourtRepository courtRepository) {
        this.courtRepository = courtRepository;
    }

    public List<CourtResponse> findAll(Surface surface) {
        List<Court> courts = (surface == null)
                ? courtRepository.findByActiveTrueOrderByName()
                : courtRepository.findBySurfaceAndActiveTrueOrderByName(surface);
        return courts.stream().map(CourtResponse::from).toList();
    }

    /** Para el panel de administración: incluye las pistas desactivadas. */
    public List<CourtResponse> findAllIncludingInactive() {
        return courtRepository.findAllByOrderByName().stream().map(CourtResponse::from).toList();
    }

    public CourtResponse findById(Long id) {
        return CourtResponse.from(getCourt(id));
    }

    @Transactional
    public CourtResponse create(CourtRequest request) {
        if (courtRepository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("Ya existe una pista con el nombre '" + request.name() + "'");
        }
        Court court = new Court(request.name(), request.surface(), request.indoor(), request.hasLighting());
        return CourtResponse.from(courtRepository.save(court));
    }

    @Transactional
    public CourtResponse update(Long id, CourtRequest request) {
        Court court = getCourt(id);
        if (courtRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new ConflictException("Ya existe una pista con el nombre '" + request.name() + "'");
        }
        court.setName(request.name());
        court.setSurface(request.surface());
        court.setIndoor(request.indoor());
        court.setHasLighting(request.hasLighting());
        // No hace falta save(): la entidad está gestionada y se guarda al cerrar la transacción
        return CourtResponse.from(court);
    }

    /**
     * Borrado lógico: la pista se desactiva en lugar de eliminarse,
     * para no perder el historial de reservas asociado.
     */
    @Transactional
    public void deactivate(Long id) {
        getCourt(id).setActive(false);
    }

    /** Vuelve a poner en servicio una pista desactivada. */
    @Transactional
    public CourtResponse activate(Long id) {
        Court court = getCourt(id);
        court.setActive(true);
        return CourtResponse.from(court);
    }

    private Court getCourt(Long id) {
        return courtRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe la pista con id " + id));
    }
}
