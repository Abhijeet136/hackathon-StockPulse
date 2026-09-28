package com.stockpulse.ai;

import com.stockpulse.domain.Direction;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmResponseParserTest {

    private final LlmResponseParser parser = new LlmResponseParser();

    @Test
    void parsesFencedJsonWithChatterAndUnknownFields() {
        String raw = """
                Sure! Here you go:
                ```json
                {"recommendedPrice": 21.49, "direction": "increase", "confidence": 0.82,
                 "reasoning": "Scarcity.", "extra": true}
                ```
                """;
        LlmPricingResponse r = parser.parsePricing(raw);
        assertThat(r.recommendedPrice()).isEqualByComparingTo("21.49");
        assertThat(r.direction()).isEqualTo(Direction.INCREASE);
        assertThat(r.reasoning()).isEqualTo("Scarcity.");
    }

    @Test
    void parsesReorder() {
        assertThat(parser.parseReorder("{\"recommendedQuantity\":150,\"confidence\":0.78,\"reasoning\":\"x\"}")
                .recommendedQuantity()).isEqualTo(150);
    }

    @Test
    void rejectsGarbage() {
        assertThatThrownBy(() -> parser.parsePricing("I cannot help with that")).isInstanceOf(LlmException.class);
        assertThatThrownBy(() -> parser.parsePricing("{not json}")).isInstanceOf(LlmException.class);
        assertThatThrownBy(() -> parser.parsePricing("")).isInstanceOf(LlmException.class);
    }
}
