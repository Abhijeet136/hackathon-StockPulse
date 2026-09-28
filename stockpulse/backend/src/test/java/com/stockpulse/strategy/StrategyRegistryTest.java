package com.stockpulse.strategy;

import com.stockpulse.exception.UnknownStrategyException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StrategyRegistryTest {

    private final PricingStrategy rulePricing = new RuleBasedPricingStrategy();
    private final PricingStrategy fakeAiPricing = ctx -> null;
    private final ReorderStrategy ruleReorder = new RuleBasedReorderStrategy();

    private StrategyRegistry registry() {
        return new StrategyRegistry(
                Map.of("pricingRuleBased", rulePricing, "pricingAi", fakeAiPricing),
                Map.of("reorderRuleBased", ruleReorder),
                "ai", "ruleBased");
    }

    @Test
    void stripsBeanPrefixesAndUsesDefaults() {
        StrategyRegistry r = registry();
        assertThat(r.pricingNames()).containsExactly("ai", "ruleBased");
        assertThat(r.activePricing()).isSameAs(fakeAiPricing);
        assertThat(r.activeReorder()).isSameAs(ruleReorder);
    }

    @Test
    void switchesAtRuntime() {
        StrategyRegistry r = registry();
        r.switchPricing("ruleBased");
        assertThat(r.activePricing()).isSameAs(rulePricing);
        assertThat(r.activePricingName()).isEqualTo("ruleBased");
    }

    @Test
    void unknownNameIsRejectedAndActiveUnchanged() {
        StrategyRegistry r = registry();
        assertThatThrownBy(() -> r.switchPricing("magic")).isInstanceOf(UnknownStrategyException.class);
        assertThat(r.activePricingName()).isEqualTo("ai");
    }
}
