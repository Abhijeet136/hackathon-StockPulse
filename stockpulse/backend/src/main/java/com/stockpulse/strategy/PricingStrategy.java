package com.stockpulse.strategy;

public interface PricingStrategy {
    PricingResult suggest(PricingContext ctx);
}
