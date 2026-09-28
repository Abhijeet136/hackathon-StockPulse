package com.stockpulse.strategy;

import com.stockpulse.domain.SuggestionSource;

import java.math.BigDecimal;

public record ReorderResult(int recommendedQuantity, int leadTimeDays, BigDecimal confidence,
                            String reasoning, SuggestionSource source) {

    public ReorderResult asFallback(String why) {
        return new ReorderResult(recommendedQuantity, leadTimeDays, confidence,
                "AI unavailable (" + why + "); rule-based: " + reasoning, SuggestionSource.RULE_FALLBACK);
    }
}
