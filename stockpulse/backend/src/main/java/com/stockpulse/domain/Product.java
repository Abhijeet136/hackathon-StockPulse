package com.stockpulse.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal currentPrice;

    @Column(nullable = false)
    private int stockLevel;

    @Column(nullable = false)
    private int reorderThreshold;

    /** Orders counted since seed; a simplified "last 24h" counter (see ADR). */
    @Column(nullable = false)
    private int demandVelocity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    /** Sprint 2: margin floor. */
    @Column(precision = 12, scale = 2)
    private BigDecimal costPrice;

    /** Sprint 2: supplier catalog. */
    private String supplierId;

    @Version
    private long version;

    protected Product() {
    }

    public Product(String sku, String name, Category category, BigDecimal currentPrice,
                   int stockLevel, int reorderThreshold, int demandVelocity) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.currentPrice = currentPrice;
        this.stockLevel = stockLevel;
        this.reorderThreshold = reorderThreshold;
        this.demandVelocity = demandVelocity;
        recomputeStatus(false);
    }

    /** One unit sold: stock -1, velocity +1. */
    public void recordSale() {
        if (stockLevel <= 0) {
            throw new IllegalStateException("Out of stock");
        }
        stockLevel--;
        demandVelocity++;
    }

    public void addStock(int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        stockLevel += qty;
    }

    public void setStockLevel(int stockLevel) {
        if (stockLevel < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }
        this.stockLevel = stockLevel;
    }

    public void updatePrice(BigDecimal price) {
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        this.currentPrice = price;
    }

    public boolean isBelowThreshold() {
        return stockLevel < reorderThreshold;
    }

    public void recomputeStatus(boolean hasPendingPricing) {
        if (stockLevel == 0) {
            status = ProductStatus.OUT_OF_STOCK;
        } else if (hasPendingPricing) {
            status = ProductStatus.PRICE_REVIEW_PENDING;
        } else {
            status = ProductStatus.ACTIVE;
        }
    }

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public Category getCategory() { return category; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public int getStockLevel() { return stockLevel; }
    public int getReorderThreshold() { return reorderThreshold; }
    public int getDemandVelocity() { return demandVelocity; }
    public ProductStatus getStatus() { return status; }
    public BigDecimal getCostPrice() { return costPrice; }
    public String getSupplierId() { return supplierId; }
    public long getVersion() { return version; }
}
