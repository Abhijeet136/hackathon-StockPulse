package com.stockpulse.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pricing_suggestion")
public class PricingSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal currentPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal recommendedPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Direction direction;

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

    protected PricingSuggestion() {
    }

    public PricingSuggestion(Product product, BigDecimal recommendedPrice, Direction direction,
                             BigDecimal confidence, String reasoning, TriggerReason triggerReason,
                             SuggestionSource source) {
        this.product = product;
        this.currentPrice = product.getCurrentPrice();
        this.recommendedPrice = recommendedPrice;
        this.direction = direction;
        this.confidence = confidence;
        this.reasoning = reasoning;
        this.triggerReason = triggerReason;
        this.source = source;
        this.status = SuggestionStatus.PENDING;
        this.dedupeKey = DedupeKeys.of(product.getSku(), triggerReason, SuggestionType.PRICING);
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
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public BigDecimal getRecommendedPrice() { return recommendedPrice; }
    public Direction getDirection() { return direction; }
    public BigDecimal getConfidence() { return confidence; }
    public String getReasoning() { return reasoning; }
    public SuggestionStatus getStatus() { return status; }
    public TriggerReason getTriggerReason() { return triggerReason; }
    public SuggestionSource getSource() { return source; }
    public String getDedupeKey() { return dedupeKey; }
    public Instant getCreatedAt() { return createdAt; }
}
