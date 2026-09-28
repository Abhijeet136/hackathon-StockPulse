package com.stockpulse.strategy;

import com.stockpulse.domain.Category;
import com.stockpulse.domain.TriggerReason;

import java.math.BigDecimal;

/** Plain snapshot of everything a pricing strategy may use. No JPA, so strategies test without a database. */
public record PricingContext(String sku, String name, Category category, BigDecimal currentPrice,
                             int stockLevel, int reorderThreshold, int demandVelocity,
                             double peerAverageVelocity, double spikeMultiplier, TriggerReason trigger) {

    public boolean isLowStock() {
        return stockLevel < reorderThreshold;
    }

    public boolean isSpiking() {
        return peerAverageVelocity > 0 && demandVelocity > spikeMultiplier * peerAverageVelocity;
    }
}
