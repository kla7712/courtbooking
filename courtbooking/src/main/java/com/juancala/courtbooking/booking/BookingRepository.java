package com.juancala.courtbooking.booking;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /** Reservas de una pista que se solapan con el intervalo [rangeStart, rangeEnd). */
    @Query("""
            select b from Booking b
            where b.court.id = :courtId
              and b.status = :status
              and b.startTime < :rangeEnd
              and b.endTime > :rangeStart
            order by b.startTime
            """)
    List<Booking> findOverlapping(@Param("courtId") Long courtId,
                                  @Param("status") BookingStatus status,
                                  @Param("rangeStart") Instant rangeStart,
                                  @Param("rangeEnd") Instant rangeEnd);

    // El EntityGraph trae la pista en la misma consulta y evita el problema N+1
    @EntityGraph(attributePaths = "court")
    List<Booking> findByUserIdOrderByStartTimeDesc(Long userId);

    long countByUserIdAndStatusAndEndTimeAfter(Long userId, BookingStatus status, Instant instant);
}
