package com.juancala.courtbooking.pricing;

import com.juancala.courtbooking.court.Surface;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceRuleRepository extends JpaRepository<PriceRule, Long> {

    List<PriceRule> findBySurfaceAndDayType(Surface surface, DayType dayType);
}
