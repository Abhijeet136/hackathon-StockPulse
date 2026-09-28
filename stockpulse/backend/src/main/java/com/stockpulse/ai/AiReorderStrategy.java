package com.stockpulse.ai;

import com.stockpulse.domain.SuggestionSource;
import com.stockpulse.strategy.ReorderContext;
import com.stockpulse.strategy.ReorderResult;
import com.stockpulse.strategy.ReorderStrategy;
import com.stockpulse.strategy.RuleBasedReorderStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;

@Component("reorderAi")
public class AiReorderStrategy implements ReorderStrategy {

    private static final Logger log = LoggerFactory.getLogger(AiReorderStrategy.class);

    private final LLMGateway gateway;
    private final PromptBuilder prompts;
    private final LlmResponseParser parser;
    private final SuggestionValidator validator;
    private final RuleBasedReorderStrategy fallback;

    public AiReorderStrategy(LLMGateway gateway, PromptBuilder prompts, LlmResponseParser parser,
                             SuggestionValidator validator, RuleBasedReorderStrategy fallback) {
        this.gateway = gateway;
        this.prompts = prompts;
        this.parser = parser;
        this.validator = validator;
        this.fallback = fallback;
    }

    @Override
    public ReorderResult suggest(ReorderContext ctx) {
        try {
            String raw = gateway.callLLM(prompts.reorderPrompt(ctx));
            LlmReorderResponse r = validator.validateReorder(parser.parseReorder(raw), ctx.reorderThreshold());
            return new ReorderResult(r.recommendedQuantity(), ctx.leadTimeDays(),
                    r.confidence().setScale(3, RoundingMode.HALF_UP), r.reasoning(), SuggestionSource.AI);
        } catch (Exception e) {
            log.warn("AI reorder failed for {} ({}): {} -> rule fallback", ctx.sku(), ctx.trigger(), e.getMessage());
            return fallback.suggest(ctx).asFallback(AiPricingStrategy.shortReason(e));
        }
    }
}
