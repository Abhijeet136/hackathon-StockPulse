package com.stockpulse.strategy;

import com.stockpulse.domain.TriggerReason;
import org.junit.jupiter.api.Test;

import static com.stockpulse.TestData.reorder;
import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedReorderStrategyTest {

    private final RuleBasedReorderStrategy strategy = new RuleBasedReorderStrategy();

    @Test
    void thresholdTimesThreeMinusStock() {
        assertThat(strategy.suggest(reorder(8, 15, 5, TriggerReason.INVENTORY_LOW)).recommendedQuantity()).isEqualTo(37);
    }

    @Test
    void coversLeadTimeDemandWhenSpiking() {
        // 11/day x 7 days = 77 > 30, so 77 - 35 = 42
        assertThat(strategy.suggest(reorder(35, 10, 11, TriggerReason.DEMAND_SPIKE)).recommendedQuantity()).isEqualTo(42);
    }

    @Test
    void neverBelowOne() {
        assertThat(strategy.suggest(reorder(500, 10, 1, TriggerReason.MANUAL)).recommendedQuantity()).isEqualTo(1);
    }
}
