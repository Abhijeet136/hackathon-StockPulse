package com.stockpulse.strategy;

import com.stockpulse.domain.Direction;
import com.stockpulse.domain.SuggestionSource;
import com.stockpulse.domain.TriggerReason;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.stockpulse.TestData.pricing;
import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedPricingStrategyTest {

    private final RuleBasedPricingStrategy strategy = new RuleBasedPricingStrategy();

    @Test
    void lowStockRaisesTenPercent() {
        PricingResult r = strategy.suggest(pricing("19.99", 8, 15, 5, 4.0, TriggerReason.INVENTORY_LOW));
        assertThat(r.recommendedPrice()).isEqualByComparingTo("21.99");
        assertThat(r.direction()).isEqualTo(Direction.INCREASE);
        assertThat(r.source()).isEqualTo(SuggestionSource.RULE_BASED);
    }

    @Test
    void spikeRaisesFivePercent() {
        PricingResult r = strategy.suggest(pricing("100.00", 40, 10, 15, 7.0, TriggerReason.DEMAND_SPIKE));
        assertThat(r.recommendedPrice()).isEqualByComparingTo("105.00");
        assertThat(r.direction()).isEqualTo(Direction.INCREASE);
    }

    @Test
    void noSignalHolds() {
        PricingResult r = strategy.suggest(pricing("10.00", 40, 10, 3, 3.0, TriggerReason.MANUAL));
        assertThat(r.recommendedPrice()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(r.direction()).isEqualTo(Direction.HOLD);
    }

    @Test
    void fallbackKeepsValuesButMarksSource() {
        PricingResult r = strategy.suggest(pricing("19.99", 8, 15, 5, 4.0, TriggerReason.INVENTORY_LOW)).asFallback("timeout");
        assertThat(r.source()).isEqualTo(SuggestionSource.RULE_FALLBACK);
        assertThat(r.reasoning()).startsWith("AI unavailable (timeout)");
    }
}
