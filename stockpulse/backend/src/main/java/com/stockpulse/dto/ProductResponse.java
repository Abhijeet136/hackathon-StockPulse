package com.stockpulse.dto;

import com.stockpulse.domain.Category;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.ProductStatus;

import java.math.BigDecimal;

public record ProductResponse(Long id, String sku, String name, Category category, BigDecimal currentPrice,
                              int stockLevel, int reorderThreshold, int demandVelocity, ProductStatus status) {

    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getSku(), p.getName(), p.getCategory(), p.getCurrentPrice(),
                p.getStockLevel(), p.getReorderThreshold(), p.getDemandVelocity(), p.getStatus());
    }
}
