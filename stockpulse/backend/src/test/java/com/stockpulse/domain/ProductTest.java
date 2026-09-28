package com.stockpulse.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    private Product product(int stock) {
        return new Product("S", "N", Category.HOME, new BigDecimal("10.00"), stock, 5, 0);
    }

    @Test
    void statusRules() {
        Product p = product(3);
        p.recomputeStatus(false);
        assertThat(p.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        p.recomputeStatus(true);
        assertThat(p.getStatus()).isEqualTo(ProductStatus.PRICE_REVIEW_PENDING);
        p.setStockLevel(0);
        p.recomputeStatus(true);
        assertThat(p.getStatus()).isEqualTo(ProductStatus.OUT_OF_STOCK);
    }

    @Test
    void saleDecrementsStockAndIncrementsVelocity() {
        Product p = product(1);
        p.recordSale();
        assertThat(p.getStockLevel()).isZero();
        assertThat(p.getDemandVelocity()).isEqualTo(1);
        assertThatThrownBy(p::recordSale).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void suggestionStateMachine() {
        PricingSuggestion s = new PricingSuggestion(product(3), new BigDecimal("11.00"), Direction.INCREASE,
                new BigDecimal("0.7"), "r", TriggerReason.MANUAL, SuggestionSource.RULE_BASED);
        assertThat(s.getDedupeKey()).isEqualTo("S:MANUAL:PRICING");
        s.accept();
        assertThat(s.getStatus()).isEqualTo(SuggestionStatus.ACCEPTED);
        assertThat(s.getDedupeKey()).isNull();
        assertThatThrownBy(s::reject).isInstanceOf(IllegalStateException.class);
    }
}
