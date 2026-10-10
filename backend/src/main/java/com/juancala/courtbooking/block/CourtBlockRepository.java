package com.juancala.courtbooking.block;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourtBlockRepository extends JpaRepository<CourtBlock, Long> {

    /** Bloqueos de una pista que se solapan con el intervalo [rangeStart, rangeEnd). */
    @Query("""
            select cb from CourtBlock cb
            where cb.court.id = :courtId
              and cb.startTime < :rangeEnd
              and cb.endTime > :rangeStart
            order by cb.startTime
            """)
    List<CourtBlock> findOverlapping(@Param("courtId") Long courtId,
                                     @Param("rangeStart") Instant rangeStart,
                                     @Param("rangeEnd") Instant rangeEnd);

    Optional<CourtBlock> findByIdAndCourtId(Long id, Long courtId);
}
