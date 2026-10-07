package com.juancala.courtbooking.block;

import com.juancala.courtbooking.block.dto.BlockResponse;
import com.juancala.courtbooking.block.dto.CreateBlockRequest;
import com.juancala.courtbooking.booking.BookingRepository;
import com.juancala.courtbooking.booking.BookingRules;
import com.juancala.courtbooking.booking.BookingStatus;
import com.juancala.courtbooking.common.BadRequestException;
import com.juancala.courtbooking.common.ConflictException;
import com.juancala.courtbooking.common.NotFoundException;
import com.juancala.courtbooking.court.Court;
import com.juancala.courtbooking.court.CourtRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CourtBlockService {

    private final CourtBlockRepository courtBlockRepository;
    private final CourtRepository courtRepository;
    private final BookingRepository bookingRepository;
    private final BookingRules rules;

    public CourtBlockService(CourtBlockRepository courtBlockRepository,
                             CourtRepository courtRepository,
                             BookingRepository bookingRepository,
                             BookingRules rules) {
        this.courtBlockRepository = courtBlockRepository;
        this.courtRepository = courtRepository;
        this.bookingRepository = bookingRepository;
        this.rules = rules;
    }

    @Transactional
    public BlockResponse create(Long courtId, CreateBlockRequest request) {
        Court court = getCourt(courtId);
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BadRequestException("La hora de fin debe ser posterior a la de inicio");
        }
        Instant start = ZonedDateTime.of(request.date(), request.startTime(), rules.getZone()).toInstant();
        Instant end = ZonedDateTime.of(request.date(), request.endTime(), rules.getZone()).toInstant();

        // No se bloquea por encima de reservas ya confirmadas: primero hay que cancelarlas
        int affected = bookingRepository
                .findOverlapping(courtId, BookingStatus.CONFIRMED, start, end).size();
        if (affected > 0) {
            throw new ConflictException("Hay " + affected
                    + " reserva(s) confirmada(s) en ese horario; cancélalas antes de bloquear la pista");
        }

        CourtBlock block = courtBlockRepository.save(
                new CourtBlock(court, start, end, request.reason().trim()));
        return BlockResponse.from(block, rules.getZone());
    }

    public List<BlockResponse> findByCourtAndDate(Long courtId, LocalDate date) {
        getCourt(courtId);
        Instant dayStart = date.atStartOfDay(rules.getZone()).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(rules.getZone()).toInstant();
        return courtBlockRepository.findOverlapping(courtId, dayStart, dayEnd).stream()
                .map(block -> BlockResponse.from(block, rules.getZone()))
                .toList();
    }

    @Transactional
    public void delete(Long courtId, Long blockId) {
        CourtBlock block = courtBlockRepository.findByIdAndCourtId(blockId, courtId)
                .orElseThrow(() -> new NotFoundException("No existe el bloqueo con id " + blockId));
        courtBlockRepository.delete(block);
    }

    private Court getCourt(Long courtId) {
        return courtRepository.findById(courtId)
                .orElseThrow(() -> new NotFoundException("No existe la pista con id " + courtId));
    }
}
