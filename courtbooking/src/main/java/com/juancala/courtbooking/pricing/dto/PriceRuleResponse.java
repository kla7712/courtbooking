package com.juancala.courtbooking.pricing.dto;

import com.juancala.courtbooking.court.Surface;
import com.juancala.courtbooking.pricing.DayType;
import com.juancala.courtbooking.pricing.PriceRule;
import java.math.BigDecimal;
import java.time.LocalTime;

public record PriceRuleResponse(
        Long id,
        Surface surface,
        DayType dayType,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal pricePerHour
) {

    public static PriceRuleResponse from(PriceRule rule) {
        return new PriceRuleResponse(rule.getId(), rule.getSurface(), rule.getDayType(),
                rule.getStartTime(), rule.getEndTime(), rule.getPricePerHour());
    }
}
