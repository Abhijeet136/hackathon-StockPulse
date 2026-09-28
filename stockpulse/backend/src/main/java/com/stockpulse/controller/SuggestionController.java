package com.stockpulse.controller;

import com.stockpulse.domain.SuggestionStatus;
import com.stockpulse.dto.DecisionRequest;
import com.stockpulse.dto.PricingSuggestionResponse;
import com.stockpulse.dto.ReorderSuggestionResponse;
import com.stockpulse.service.SuggestionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SuggestionController {

    private final SuggestionService suggestions;

    public SuggestionController(SuggestionService suggestions) {
        this.suggestions = suggestions;
    }

    @GetMapping("/pricing-suggestions")
    public List<PricingSuggestionResponse> pricing(@RequestParam(required = false) SuggestionStatus status) {
        return suggestions.pricingSuggestions(status).stream().map(PricingSuggestionResponse::from).toList();
    }

    @GetMapping("/reorder-suggestions")
    public List<ReorderSuggestionResponse> reorder(@RequestParam(required = false) SuggestionStatus status) {
        return suggestions.reorderSuggestions(status).stream().map(ReorderSuggestionResponse::from).toList();
    }

    @PatchMapping("/pricing-suggestions/{id}")
    public PricingSuggestionResponse decidePricing(@PathVariable Long id, @Valid @RequestBody DecisionRequest request) {
        return PricingSuggestionResponse.from(suggestions.decidePricing(id, request.status()));
    }

    @PatchMapping("/reorder-suggestions/{id}")
    public ReorderSuggestionResponse decideReorder(@PathVariable Long id, @Valid @RequestBody DecisionRequest request) {
        return ReorderSuggestionResponse.from(suggestions.decideReorder(id, request.status()));
    }
}
