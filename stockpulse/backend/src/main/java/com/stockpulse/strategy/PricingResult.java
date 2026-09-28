package com.stockpulse.strategy;

import com.stockpulse.domain.Direction;
import com.stockpulse.domain.SuggestionSource;

import java.math.BigDecimal;

public record PricingResult(BigDecimal recommendedPrice, Direction direction, BigDecimal confidence,
                            String reasoning, SuggestionSource source) {

    public PricingResult asFallback(String why) {
        return new PricingResult(recommendedPrice, direction, confidence,
                "AI unavailable (" + why + "); rule-based: " + reasoning, SuggestionSource.RULE_FALLBACK);
    }
}
