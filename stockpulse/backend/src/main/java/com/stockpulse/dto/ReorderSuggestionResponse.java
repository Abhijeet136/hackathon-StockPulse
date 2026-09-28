package com.stockpulse.dto;

import com.stockpulse.domain.*;

import java.math.BigDecimal;
import java.time.Instant;

public record ReorderSuggestionResponse(Long id, Long productId, String sku, String productName,
                                        int currentStock, int recommendedQuantity, int leadTimeDays,
                                        BigDecimal confidence, String reasoning, SuggestionStatus status,
                                        TriggerReason triggerReason, SuggestionSource source, Instant createdAt) {

    public static ReorderSuggestionResponse from(ReorderSuggestion s) {
        Product p = s.getProduct();
        return new ReorderSuggestionResponse(s.getId(), p.getId(), p.getSku(), p.getName(),
                s.getCurrentStock(), s.getRecommendedQuantity(), s.getLeadTimeDays(), s.getConfidence(),
                s.getReasoning(), s.getStatus(), s.getTriggerReason(), s.getSource(), s.getCreatedAt());
    }
}
