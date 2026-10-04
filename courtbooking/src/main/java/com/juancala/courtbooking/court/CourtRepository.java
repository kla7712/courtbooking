package com.juancala.courtbooking.court;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourtRepository extends JpaRepository<Court, Long> {

    List<Court> findByActiveTrueOrderByName();

    List<Court> findBySurfaceAndActiveTrueOrderByName(Surface surface);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
