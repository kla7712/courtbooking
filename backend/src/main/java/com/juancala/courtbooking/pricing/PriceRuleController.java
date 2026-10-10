package com.juancala.courtbooking.pricing;

import com.juancala.courtbooking.pricing.dto.PriceRuleResponse;
import com.juancala.courtbooking.pricing.dto.UpdatePriceRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/price-rules")
public class PriceRuleController {

    private final PricingService pricingService;

    public PriceRuleController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    /** Público: las tarifas se pueden consultar sin iniciar sesión. */
    @GetMapping
    public List<PriceRuleResponse> findAll() {
        return pricingService.findAll();
    }

    /** Solo admin: cambia el precio por hora de una franja. */
    @PutMapping("/{id}")
    public PriceRuleResponse updatePrice(@PathVariable Long id, @Valid @RequestBody UpdatePriceRequest request) {
        return pricingService.updatePrice(id, request.pricePerHour());
    }
}
