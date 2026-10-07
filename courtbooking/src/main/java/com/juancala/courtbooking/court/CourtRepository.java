package com.juancala.courtbooking.court;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourtRepository extends JpaRepository<Court, Long> {

    List<Court> findByActiveTrueOrderByName();

    List<Court> findBySurfaceAndActiveTrueOrderByName(Surface surface);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /**
     * Lee la pista bloqueando su fila (SELECT ... FOR UPDATE) hasta que termine la transacción.
     * Sirve para que las reservas y bloqueos de una misma pista se procesen de uno en uno.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Court c where c.id = :id")
    Optional<Court> findByIdForUpdate(@Param("id") Long id);
}
