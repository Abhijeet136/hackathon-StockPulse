package com.stockpulse;

import com.stockpulse.domain.Category;
import com.stockpulse.domain.TriggerReason;
import com.stockpulse.strategy.PricingContext;
import com.stockpulse.strategy.ReorderContext;

import java.math.BigDecimal;

public final class TestData {

    private TestData() {
    }

    public static PricingContext pricing(String price, int stock, int threshold, int velocity, double peerAvg,
                                         TriggerReason trigger) {
        return new PricingContext("PRD-T", "Test Tee", Category.APPAREL, new BigDecimal(price), stock, threshold,
                velocity, peerAvg, 2.0, trigger);
    }

    public static ReorderContext reorder(int stock, int threshold, int velocity, TriggerReason trigger) {
        return new ReorderContext("PRD-T", "Test Tee", Category.APPAREL, stock, threshold, velocity, 3.0, 7, trigger);
    }
}
