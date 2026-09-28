package com.stockpulse.dto;

import com.stockpulse.domain.*;

import java.math.BigDecimal;
import java.time.Instant;

public record PricingSuggestionResponse(Long id, Long productId, String sku, String productName,
                                        BigDecimal currentPrice, BigDecimal recommendedPrice, Direction direction,
                                        BigDecimal confidence, String reasoning, SuggestionStatus status,
                                        TriggerReason triggerReason, SuggestionSource source, Instant createdAt) {

    public static PricingSuggestionResponse from(PricingSuggestion s) {
        Product p = s.getProduct();
        return new PricingSuggestionResponse(s.getId(), p.getId(), p.getSku(), p.getName(),
                s.getCurrentPrice(), s.getRecommendedPrice(), s.getDirection(), s.getConfidence(),
                s.getReasoning(), s.getStatus(), s.getTriggerReason(), s.getSource(), s.getCreatedAt());
    }
}
