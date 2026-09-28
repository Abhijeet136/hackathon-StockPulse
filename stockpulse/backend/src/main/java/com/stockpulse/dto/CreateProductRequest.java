package com.stockpulse.dto;

import com.stockpulse.domain.Category;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank @Size(max = 32) String sku,
        @NotBlank @Size(max = 200) String name,
        @NotNull Category category,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal currentPrice,
        @NotNull @PositiveOrZero Integer stockLevel,
        @NotNull @PositiveOrZero Integer reorderThreshold,
        @PositiveOrZero Integer demandVelocity) {
}
