package com.stockpulse.ai;

import com.stockpulse.domain.Direction;

import java.math.BigDecimal;

public record LlmPricingResponse(BigDecimal recommendedPrice, Direction direction, BigDecimal confidence,
                                 String reasoning) {
}
