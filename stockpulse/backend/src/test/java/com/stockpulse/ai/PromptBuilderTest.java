package com.stockpulse.ai;

import com.stockpulse.domain.TriggerReason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static com.stockpulse.TestData.pricing;
import static com.stockpulse.TestData.reorder;
import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    private final PromptBuilder builder = new PromptBuilder();

    @ParameterizedTest
    @EnumSource(TriggerReason.class)
    void allPlaceholdersAreFilled(TriggerReason trigger) {
        String p = builder.pricingPrompt(pricing("19.99", 8, 15, 5, 4.0, trigger));
        String r = builder.reorderPrompt(reorder(8, 15, 5, trigger));
        assertThat(p).doesNotContainPattern("\\{[a-zA-Z]+\\}").contains("19.99").contains("recommendedPrice");
        assertThat(r).doesNotContainPattern("\\{[a-zA-Z]+\\}").contains("recommendedQuantity");
    }

    @Test
    void lowStockAndSpikePromptsDiffer() {
        String low = builder.pricingPrompt(pricing("19.99", 8, 15, 5, 4.0, TriggerReason.INVENTORY_LOW));
        String spike = builder.pricingPrompt(pricing("19.99", 8, 15, 5, 4.0, TriggerReason.DEMAND_SPIKE));
        assertThat(low).contains("SITUATION: LOW STOCK").contains("clearance");
        assertThat(spike).contains("SITUATION: DEMAND SPIKE").contains("one-off");
    }
}
