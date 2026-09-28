package com.stockpulse.domain;

/** Builds keys like {@code PRD-003:INVENTORY_LOW:PRICING}. */
public final class DedupeKeys {

    private DedupeKeys() {
    }

    public static String of(String sku, TriggerReason reason, SuggestionType type) {
        return sku + ":" + reason + ":" + type;
    }
}
