package com.stockpulse.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Body is optional; quantity defaults to 1. */
public record OrderRequest(@Min(1) @Max(100) Integer quantity) {

    public int quantityOrDefault() {
        return quantity == null ? 1 : quantity;
    }
}
