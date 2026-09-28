package com.stockpulse.ai;

import java.math.BigDecimal;

public record LlmReorderResponse(Integer recommendedQuantity, BigDecimal confidence, String reasoning) {
}
