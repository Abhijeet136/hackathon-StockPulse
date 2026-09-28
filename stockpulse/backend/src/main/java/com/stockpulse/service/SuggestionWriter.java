package com.stockpulse.service;

import com.stockpulse.domain.PricingSuggestion;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.ReorderSuggestion;
import com.stockpulse.domain.SuggestionStatus;
import com.stockpulse.domain.TriggerReason;
import com.stockpulse.exception.NotFoundException;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import com.stockpulse.strategy.PricingResult;
import com.stockpulse.strategy.ReorderResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Short write transactions, kept separate from SuggestionService so the LLM call never holds a DB transaction.
 * A DataIntegrityViolationException from the unique dedupe_key propagates to the caller, which treats it as a duplicate.
 */
@Component
public class SuggestionWriter {

    private final ProductRepository products;
    private final PricingSuggestionRepository pricing;
    private final ReorderSuggestionRepository reorder;

    public SuggestionWriter(ProductRepository products, PricingSuggestionRepository pricing,
                            ReorderSuggestionRepository reorder) {
        this.products = products;
        this.pricing = pricing;
        this.reorder = reorder;
    }

    @Transactional
    public Optional<PricingSuggestion> savePricing(Long productId, TriggerReason reason, PricingResult r) {
        Product p = product(productId);
        if (pricing.existsByProductIdAndTriggerReasonAndStatus(productId, reason, SuggestionStatus.PENDING)) {
            return Optional.empty();
        }
        PricingSuggestion s = pricing.saveAndFlush(new PricingSuggestion(p, r.recommendedPrice(), r.direction(),
                r.confidence(), r.reasoning(), reason, r.source()));
        p.recomputeStatus(true);
        return Optional.of(s);
    }

    @Transactional
    public Optional<ReorderSuggestion> saveReorder(Long productId, TriggerReason reason, ReorderResult r) {
        Product p = product(productId);
        if (reorder.existsByProductIdAndTriggerReasonAndStatus(productId, reason, SuggestionStatus.PENDING)) {
            return Optional.empty();
        }
        return Optional.of(reorder.saveAndFlush(new ReorderSuggestion(p, r.recommendedQuantity(), r.leadTimeDays(),
                r.confidence(), r.reasoning(), reason, r.source())));
    }

    private Product product(Long id) {
        return products.findById(id).orElseThrow(() -> new NotFoundException("Product " + id + " not found"));
    }
}
