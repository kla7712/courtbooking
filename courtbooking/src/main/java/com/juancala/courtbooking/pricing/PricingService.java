package com.juancala.courtbooking.pricing;

import com.juancala.courtbooking.booking.BookingRules;
import com.juancala.courtbooking.common.BadRequestException;
import com.juancala.courtbooking.common.NotFoundException;
import com.juancala.courtbooking.court.Court;
import com.juancala.courtbooking.pricing.dto.PriceRuleResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PricingService {

    private static final BigDecimal SEGMENTS_PER_HOUR =
            BigDecimal.valueOf(60 / BookingRules.SLOT_STEP_MINUTES);

    private final PriceRuleRepository priceRuleRepository;
    private final BookingRules rules;

    public PricingService(PriceRuleRepository priceRuleRepository, BookingRules rules) {
        this.priceRuleRepository = priceRuleRepository;
        this.rules = rules;
    }

    /** Tarifas aplicables a una pista en una fecha (según su superficie y si es fin de semana). */
    public List<PriceRule> loadRules(Court court, LocalDate date) {
        return priceRuleRepository.findBySurfaceAndDayType(court.getSurface(), DayType.of(date));
    }

    public BigDecimal calculate(Court court, LocalDate date, LocalTime startTime, int durationMinutes) {
        return calculate(loadRules(court, date), court, startTime, durationMinutes);
    }

    /**
     * Calcula el precio tramo a tramo de media hora, porque una reserva puede cruzar
     * de una franja a otra (p. ej. de valle a punta) o entrar en horario con luz.
     */
    public BigDecimal calculate(List<PriceRule> priceRules, Court court, LocalTime startTime, int durationMinutes) {
        int startMinute = startTime.toSecondOfDay() / 60;
        int lightingFromMinute = rules.getLightingFrom().toSecondOfDay() / 60;

        BigDecimal total = BigDecimal.ZERO;
        for (int minute = startMinute; minute < startMinute + durationMinutes;
             minute += BookingRules.SLOT_STEP_MINUTES) {
            LocalTime segment = LocalTime.ofSecondOfDay(minute * 60L);
            BigDecimal hourlyPrice = priceRules.stream()
                    .filter(rule -> rule.covers(segment))
                    .findFirst()
                    .map(PriceRule::getPricePerHour)
                    .orElseThrow(() -> new BadRequestException("No hay tarifa configurada para las " + segment));
            if (court.isHasLighting() && minute >= lightingFromMinute) {
                hourlyPrice = hourlyPrice.add(rules.getLightingSupplementPerHour());
            }
            total = total.add(hourlyPrice.divide(SEGMENTS_PER_HOUR, 4, RoundingMode.HALF_UP));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    public List<PriceRuleResponse> findAll() {
        return priceRuleRepository.findAll(Sort.by("surface", "dayType", "startTime")).stream()
                .map(PriceRuleResponse::from)
                .toList();
    }

    @Transactional
    public PriceRuleResponse updatePrice(Long id, BigDecimal pricePerHour) {
        PriceRule rule = priceRuleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe la tarifa con id " + id));
        rule.setPricePerHour(pricePerHour);
        return PriceRuleResponse.from(rule);
    }
}
