package com.stockpulse.ai;

import com.stockpulse.domain.Direction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SuggestionValidatorTest {

    private final SuggestionValidator validator = new SuggestionValidator();
    private final BigDecimal current = new BigDecimal("20.00");

    private static LlmPricingResponse pricing(String price, Direction d, String confidence) {
        return new LlmPricingResponse(new BigDecimal(price), d, new BigDecimal(confidence), "because");
    }

    @Test
    void acceptsSaneIncrease() {
        assertThat(validator.validatePricing(pricing("21.999", Direction.INCREASE, "0.8"), current).recommendedPrice())
                .isEqualByComparingTo("22.00");
    }

    @Test
    void rejectsMoreThanFiftyPercent() {
        assertThatThrownBy(() -> validator.validatePricing(pricing("30.01", Direction.INCREASE, "0.8"), current))
                .isInstanceOf(LlmException.class);
        assertThatThrownBy(() -> validator.validatePricing(pricing("9.99", Direction.DECREASE, "0.8"), current))
                .isInstanceOf(LlmException.class);
    }

    @Test
    void rejectsDirectionMismatchBadConfidenceAndMissingFields() {
        assertThatThrownBy(() -> validator.validatePricing(pricing("22.00", Direction.DECREASE, "0.8"), current))
                .isInstanceOf(LlmException.class);
        assertThatThrownBy(() -> validator.validatePricing(pricing("20.00", Direction.INCREASE, "0.8"), current))
                .isInstanceOf(LlmException.class);
        assertThatThrownBy(() -> validator.validatePricing(pricing("22.00", Direction.INCREASE, "1.2"), current))
                .isInstanceOf(LlmException.class);
        assertThatThrownBy(() -> validator.validatePricing(new LlmPricingResponse(null, Direction.HOLD, BigDecimal.ONE, "x"), current))
                .isInstanceOf(LlmException.class);
    }

    @Test
    void reorderQuantityBounds() {
        assertThat(validator.validateReorder(new LlmReorderResponse(150, new BigDecimal("0.7"), "x"), 15).recommendedQuantity())
                .isEqualTo(150);
        assertThatThrownBy(() -> validator.validateReorder(new LlmReorderResponse(0, new BigDecimal("0.7"), "x"), 15))
                .isInstanceOf(LlmException.class);
        assertThatThrownBy(() -> validator.validateReorder(new LlmReorderResponse(151, new BigDecimal("0.7"), "x"), 15))
                .isInstanceOf(LlmException.class);
    }
}
