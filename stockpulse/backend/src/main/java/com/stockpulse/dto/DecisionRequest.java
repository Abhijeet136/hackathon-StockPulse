package com.stockpulse.dto;

import com.stockpulse.domain.SuggestionStatus;
import jakarta.validation.constraints.NotNull;

/** status must be ACCEPTED or REJECTED. */
public record DecisionRequest(@NotNull SuggestionStatus status) {
}
