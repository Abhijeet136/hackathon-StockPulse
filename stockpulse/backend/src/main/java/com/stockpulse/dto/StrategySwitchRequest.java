package com.stockpulse.dto;

import jakarta.validation.constraints.Pattern;

/** Either field may be omitted to leave that strategy unchanged. Values: "ai" or "ruleBased". */
public record StrategySwitchRequest(
        @Pattern(regexp = "\\w+") String pricing,
        @Pattern(regexp = "\\w+") String reorder) {
}
