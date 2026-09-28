package com.stockpulse.ai;

import com.stockpulse.domain.Direction;
import com.stockpulse.domain.SuggestionSource;
import com.stockpulse.domain.TriggerReason;
import com.stockpulse.strategy.PricingResult;
import com.stockpulse.strategy.ReorderResult;
import com.stockpulse.strategy.RuleBasedPricingStrategy;
import com.stockpulse.strategy.RuleBasedReorderStrategy;
import org.junit.jupiter.api.Test;

import static com.stockpulse.TestData.pricing;
import static com.stockpulse.TestData.reorder;
import static org.assertj.core.api.Assertions.assertThat;

class AiStrategyFallbackTest {

    private AiPricingStrategy pricingWith(LLMGateway gateway) {
        return new AiPricingStrategy(gateway, new PromptBuilder(), new LlmResponseParser(), new SuggestionValidator(),
                new RuleBasedPricingStrategy());
    }

    @Test
    void usesAiWhenResponseIsValid() {
        PricingResult r = pricingWith(p -> "{\"recommendedPrice\":21.49,\"direction\":\"INCREASE\",\"confidence\":0.82,\"reasoning\":\"Scarcity\"}")
                .suggest(pricing("19.99", 8, 15, 5, 4.0, TriggerReason.INVENTORY_LOW));
        assertThat(r.source()).isEqualTo(SuggestionSource.AI);
        assertThat(r.recommendedPrice()).isEqualByComparingTo("21.49");
    }

    @Test
    void fallsBackWhenGatewayThrows() {
        PricingResult r = pricingWith(p -> { throw new LlmException("Gemini HTTP 429"); })
                .suggest(pricing("19.99", 8, 15, 5, 4.0, TriggerReason.INVENTORY_LOW));
        assertThat(r.source()).isEqualTo(SuggestionSource.RULE_FALLBACK);
        assertThat(r.recommendedPrice()).isEqualByComparingTo("21.99");
        assertThat(r.reasoning()).contains("429");
    }

    @Test
    void fallsBackOnBadJsonOrCrazyValue() {
        assertThat(pricingWith(p -> "not json").suggest(pricing("19.99", 8, 15, 5, 4.0, TriggerReason.INVENTORY_LOW)).source())
                .isEqualTo(SuggestionSource.RULE_FALLBACK);
        PricingResult crazy = pricingWith(p -> "{\"recommendedPrice\":999,\"direction\":\"INCREASE\",\"confidence\":0.9,\"reasoning\":\"x\"}")
                .suggest(pricing("19.99", 8, 15, 5, 4.0, TriggerReason.INVENTORY_LOW));
        assertThat(crazy.source()).isEqualTo(SuggestionSource.RULE_FALLBACK);
        assertThat(crazy.direction()).isEqualTo(Direction.INCREASE);
    }

    @Test
    void reorderFallsBack() {
        AiReorderStrategy s = new AiReorderStrategy(p -> { throw new LlmException("timeout"); }, new PromptBuilder(),
                new LlmResponseParser(), new SuggestionValidator(), new RuleBasedReorderStrategy());
        ReorderResult r = s.suggest(reorder(8, 15, 5, TriggerReason.INVENTORY_LOW));
        assertThat(r.source()).isEqualTo(SuggestionSource.RULE_FALLBACK);
        assertThat(r.recommendedQuantity()).isEqualTo(37);
    }
}
