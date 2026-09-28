package com.stockpulse.ai;

import com.stockpulse.domain.Direction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Safety limits the LLM must stay within. A violation throws LlmException, which triggers the fallback. */
@Component
public class SuggestionValidator {

    static final BigDecimal MAX_CHANGE = new BigDecimal("0.50");
    static final int MAX_REASONING = 1000;

    public LlmPricingResponse validatePricing(LlmPricingResponse r, BigDecimal currentPrice) {
        if (r.recommendedPrice() == null || r.direction() == null || r.confidence() == null) {
            throw new LlmException("Missing pricing field(s)");
        }
        BigDecimal price = r.recommendedPrice().setScale(2, RoundingMode.HALF_UP);
        if (price.signum() <= 0) {
            throw new LlmException("Non-positive price " + price);
        }
        BigDecimal change = price.subtract(currentPrice).abs().divide(currentPrice, 4, RoundingMode.HALF_UP);
        if (change.compareTo(MAX_CHANGE) > 0) {
            throw new LlmException("Price " + price + " is more than 50% away from " + currentPrice);
        }
        checkConfidence(r.confidence());
        Direction expected = switch (price.compareTo(currentPrice)) {
            case 1 -> Direction.INCREASE;
            case -1 -> Direction.DECREASE;
            default -> Direction.HOLD;
        };
        if (r.direction() != expected) {
            throw new LlmException("Direction " + r.direction() + " does not match price change (expected " + expected + ")");
        }
        return new LlmPricingResponse(price, r.direction(), r.confidence(), reasoning(r.reasoning()));
    }

    public LlmReorderResponse validateReorder(LlmReorderResponse r, int reorderThreshold) {
        if (r.recommendedQuantity() == null || r.confidence() == null) {
            throw new LlmException("Missing reorder field(s)");
        }
        int max = maxReorderQuantity(reorderThreshold);
        if (r.recommendedQuantity() <= 0 || r.recommendedQuantity() > max) {
            throw new LlmException("Quantity " + r.recommendedQuantity() + " outside 1.." + max);
        }
        checkConfidence(r.confidence());
        return new LlmReorderResponse(r.recommendedQuantity(), r.confidence(), reasoning(r.reasoning()));
    }

    public static int maxReorderQuantity(int reorderThreshold) {
        return Math.max(1, reorderThreshold) * 10;
    }

    private static void checkConfidence(BigDecimal c) {
        if (c.compareTo(BigDecimal.ZERO) < 0 || c.compareTo(BigDecimal.ONE) > 0) {
            throw new LlmException("Confidence " + c + " outside 0..1");
        }
    }

    private static String reasoning(String text) {
        if (text == null || text.isBlank()) {
            throw new LlmException("Missing reasoning");
        }
        String trimmed = text.strip();
        return trimmed.length() > MAX_REASONING ? trimmed.substring(0, MAX_REASONING) : trimmed;
    }
}
