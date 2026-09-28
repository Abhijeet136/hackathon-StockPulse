package com.stockpulse.strategy;

import com.stockpulse.domain.Category;
import com.stockpulse.domain.TriggerReason;

public record ReorderContext(String sku, String name, Category category, int stockLevel, int reorderThreshold,
                             int demandVelocity, double peerAverageVelocity, int leadTimeDays,
                             TriggerReason trigger) {
}
