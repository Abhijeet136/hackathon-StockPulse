package com.stockpulse.controller;

import com.stockpulse.dto.StrategyResponse;
import com.stockpulse.dto.StrategySwitchRequest;
import com.stockpulse.strategy.StrategyRegistry;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/strategy")
public class AdminController {

    private final StrategyRegistry registry;

    public AdminController(StrategyRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public StrategyResponse current() {
        return new StrategyResponse(registry.activePricingName(), registry.activeReorderName(),
                registry.pricingNames(), registry.reorderNames());
    }

    @PutMapping
    public StrategyResponse switchStrategy(@Valid @RequestBody StrategySwitchRequest request) {
        if (request.pricing() != null) {
            registry.switchPricing(request.pricing());
        }
        if (request.reorder() != null) {
            registry.switchReorder(request.reorder());
        }
        return current();
    }
}
