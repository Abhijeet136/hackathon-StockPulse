package com.stockpulse.strategy;

import com.stockpulse.domain.SuggestionSource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Order enough to reach the larger of three times the threshold or the demand expected during the lead time:
 * max(1, max(threshold x 3, velocity x leadTimeDays) - stock). With low velocity this is the classic threshold x 3 - stock.
 */
@Component("reorderRuleBased")
public class RuleBasedReorderStrategy implements ReorderStrategy {

    @Override
    public ReorderResult suggest(ReorderContext ctx) {
        int thresholdTarget = ctx.reorderThreshold() * 3;
        int leadTimeDemand = ctx.demandVelocity() * ctx.leadTimeDays();
        String reasoning;
        int qty;
        if (leadTimeDemand > thresholdTarget) {
            qty = Math.max(1, leadTimeDemand - ctx.stockLevel());
            reasoning = "Cover lead-time demand: %d/day x %d days - %d in stock = %d units."
                    .formatted(ctx.demandVelocity(), ctx.leadTimeDays(), ctx.stockLevel(), qty);
        } else {
            qty = Math.max(1, thresholdTarget - ctx.stockLevel());
            reasoning = "Restock to 3x threshold: %d x 3 - %d = %d units."
                    .formatted(ctx.reorderThreshold(), ctx.stockLevel(), qty);
        }
        return new ReorderResult(qty, ctx.leadTimeDays(), new BigDecimal("0.70"), reasoning, SuggestionSource.RULE_BASED);
    }
}
