package com.stockpulse.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "reorder_suggestion")
public class ReorderSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private int currentStock;

    @Column(nullable = false)
    private int recommendedQuantity;

    @Column(nullable = false)
    private int leadTimeDays;

    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(nullable = false, columnDefinition = "text")
    private String reasoning;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SuggestionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TriggerReason triggerReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SuggestionSource source;

    @Column(unique = true)
    private String dedupeKey;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected ReorderSuggestion() {
    }

    public ReorderSuggestion(Product product, int recommendedQuantity, int leadTimeDays,
                             BigDecimal confidence, String reasoning, TriggerReason triggerReason,
                             SuggestionSource source) {
        this.product = product;
        this.currentStock = product.getStockLevel();
        this.recommendedQuantity = recommendedQuantity;
        this.leadTimeDays = leadTimeDays;
        this.confidence = confidence;
        this.reasoning = reasoning;
        this.triggerReason = triggerReason;
        this.source = source;
        this.status = SuggestionStatus.PENDING;
        this.dedupeKey = DedupeKeys.of(product.getSku(), triggerReason, SuggestionType.REORDER);
        this.createdAt = Instant.now();
    }

    public void accept() {
        requirePending();
        status = SuggestionStatus.ACCEPTED;
        dedupeKey = null;
    }

    public void reject() {
        requirePending();
        status = SuggestionStatus.REJECTED;
        dedupeKey = null;
    }

    private void requirePending() {
        if (status != SuggestionStatus.PENDING) {
            throw new IllegalStateException("Suggestion " + id + " is already " + status);
        }
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public int getCurrentStock() { return currentStock; }
    public int getRecommendedQuantity() { return recommendedQuantity; }
    public int getLeadTimeDays() { return leadTimeDays; }
    public BigDecimal getConfidence() { return confidence; }
    public String getReasoning() { return reasoning; }
    public SuggestionStatus getStatus() { return status; }
    public TriggerReason getTriggerReason() { return triggerReason; }
    public SuggestionSource getSource() { return source; }
    public String getDedupeKey() { return dedupeKey; }
    public Instant getCreatedAt() { return createdAt; }
}
