package com.stockpulse.ai;

import com.stockpulse.domain.SuggestionSource;
import com.stockpulse.strategy.PricingContext;
import com.stockpulse.strategy.PricingResult;
import com.stockpulse.strategy.PricingStrategy;
import com.stockpulse.strategy.RuleBasedPricingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

/** prompt -> LLM -> parse -> validate; any failure falls back to rules with source RULE_FALLBACK. */
@Component("pricingAi")
public class AiPricingStrategy implements PricingStrategy {

    private static final Logger log = LoggerFactory.getLogger(AiPricingStrategy.class);

    private final LLMGateway gateway;
    private final PromptBuilder prompts;
    private final LlmResponseParser parser;
    private final SuggestionValidator validator;
    private final RuleBasedPricingStrategy fallback;

    public AiPricingStrategy(LLMGateway gateway, PromptBuilder prompts, LlmResponseParser parser,
                             SuggestionValidator validator, RuleBasedPricingStrategy fallback) {
        this.gateway = gateway;
        this.prompts = prompts;
        this.parser = parser;
        this.validator = validator;
        this.fallback = fallback;
    }

    @Override
    public PricingResult suggest(PricingContext ctx) {
        try {
            String raw = gateway.callLLM(prompts.pricingPrompt(ctx));
            LlmPricingResponse r = validator.validatePricing(parser.parsePricing(raw), ctx.currentPrice());
            return new PricingResult(r.recommendedPrice(), r.direction(),
                    r.confidence().setScale(3, RoundingMode.HALF_UP), r.reasoning(), SuggestionSource.AI);
        } catch (Exception e) {
            log.warn("AI pricing failed for {} ({}): {} -> rule fallback", ctx.sku(), ctx.trigger(), e.getMessage());
            return fallback.suggest(ctx).asFallback(shortReason(e));
        }
    }

    static String shortReason(Exception e) {
        String m = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return m.length() > 120 ? m.substring(0, 120) + "..." : m;
    }
}
