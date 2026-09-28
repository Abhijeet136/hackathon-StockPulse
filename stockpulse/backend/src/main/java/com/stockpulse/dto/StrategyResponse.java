package com.stockpulse.dto;

import java.util.Set;

public record StrategyResponse(String pricing, String reorder,
                               Set<String> availablePricing, Set<String> availableReorder) {
}
