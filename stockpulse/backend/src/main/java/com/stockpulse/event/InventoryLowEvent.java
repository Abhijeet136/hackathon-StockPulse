package com.stockpulse.event;

/** Carries only the ID; listeners reload committed state. */
public record InventoryLowEvent(Long productId) {
}
