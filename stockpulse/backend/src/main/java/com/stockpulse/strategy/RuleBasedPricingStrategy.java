package com.stockpulse.strategy;

import com.stockpulse.domain.Direction;
import com.stockpulse.domain.SuggestionSource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Deterministic rules: low stock → +10%, demand spike → +5%, otherwise hold.
 * Also the fallback for the AI strategy.
 */
@Component("pricingRuleBased")
public class RuleBasedPricingStrategy implements PricingStrategy {

    private static final BigDecimal LOW_STOCK_UPLIFT = new BigDecimal("1.10");
    private static final BigDecimal SPIKE_UPLIFT = new BigDecimal("1.05");

    @Override
    public PricingResult suggest(PricingContext ctx) {
        if (ctx.isLowStock()) {
            return increase(ctx, LOW_STOCK_UPLIFT, "0.70",
                    "Stock %d is below threshold %d; +10%% protects remaining units until restock."
                            .formatted(ctx.stockLevel(), ctx.reorderThreshold()));
        }
        if (ctx.isSpiking()) {
            return increase(ctx, SPIKE_UPLIFT, "0.60",
                    "Velocity %d exceeds %.1fx the category peer average (%.1f); +5%% captures demand."
                            .formatted(ctx.demandVelocity(), ctx.spikeMultiplier(), ctx.peerAverageVelocity()));
        }
        return new PricingResult(ctx.currentPrice(), Direction.HOLD, new BigDecimal("0.50"),
                "No low-stock or demand-spike signal; hold the current price.", SuggestionSource.RULE_BASED);
    }

    private PricingResult increase(PricingContext ctx, BigDecimal factor, String confidence, String reasoning) {
        BigDecimal price = ctx.currentPrice().multiply(factor).setScale(2, RoundingMode.HALF_UP);
        return new PricingResult(price, Direction.INCREASE, new BigDecimal(confidence), reasoning,
                SuggestionSource.RULE_BASED);
    }
}
