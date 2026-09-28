package com.stockpulse.service;

import com.stockpulse.domain.Category;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.ProductStatus;
import com.stockpulse.domain.SuggestionStatus;
import com.stockpulse.dto.CreateProductRequest;
import com.stockpulse.event.DemandSpikeEvent;
import com.stockpulse.event.InventoryLowEvent;
import com.stockpulse.exception.ConflictException;
import com.stockpulse.exception.NotFoundException;
import com.stockpulse.exception.OutOfStockException;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository products;
    private final PricingSuggestionRepository pricingSuggestions;
    private final ContextFactory contexts;
    private final ApplicationEventPublisher events;

    public ProductService(ProductRepository products, PricingSuggestionRepository pricingSuggestions,
                          ContextFactory contexts, ApplicationEventPublisher events) {
        this.products = products;
        this.pricingSuggestions = pricingSuggestions;
        this.contexts = contexts;
        this.events = events;
    }

    @Transactional
    public Product create(CreateProductRequest req) {
        if (products.existsBySku(req.sku())) {
            throw new ConflictException("SKU " + req.sku() + " already exists");
        }
        Product p = new Product(req.sku(), req.name(), req.category(), req.currentPrice(), req.stockLevel(),
                req.reorderThreshold(), req.demandVelocity() == null ? 0 : req.demandVelocity());
        return products.save(p);
    }

    @Transactional(readOnly = true)
    public List<Product> list(ProductStatus status, Category category) {
        return products.search(status, category);
    }

    @Transactional(readOnly = true)
    public Product get(Long id) {
        return find(id);
    }

    /**
     * Simulated sale. Returns immediately; suggestions are produced by listeners after commit.
     */
    @Transactional
    public Product placeOrder(Long id, int quantity) {
        Product p = find(id);
        if (p.getStockLevel() <= 0) {
            throw new OutOfStockException("Out of stock");
        }
        if (quantity > p.getStockLevel()) {
            throw new OutOfStockException("Only " + p.getStockLevel() + " unit(s) in stock");
        }
        for (int i = 0; i < quantity; i++) {
            p.recordSale();
        }
        p.recomputeStatus(hasPendingPricing(p));
        publishSignals(p);
        return p;
    }

    @Transactional
    public Product updateStock(Long id, int stockLevel) {
        Product p = find(id);
        p.setStockLevel(stockLevel);
        p.recomputeStatus(hasPendingPricing(p));
        if (p.isBelowThreshold()) {
            log.info("{} stock set to {} (threshold {}) -> INVENTORY_LOW", p.getSku(), stockLevel, p.getReorderThreshold());
            events.publishEvent(new InventoryLowEvent(p.getId()));
        }
        return p;
    }

    private void publishSignals(Product p) {
        if (p.isBelowThreshold()) {
            log.info("{} stock {} < threshold {} -> INVENTORY_LOW", p.getSku(), p.getStockLevel(), p.getReorderThreshold());
            events.publishEvent(new InventoryLowEvent(p.getId()));
        }
        if (contexts.isSpiking(p)) {
            log.info("{} velocity {} > {} x peers avg {} -> DEMAND_SPIKE", p.getSku(), p.getDemandVelocity(),
                    contexts.spikeMultiplier(), String.format("%.2f", contexts.peerAverageVelocity(p)));
            events.publishEvent(new DemandSpikeEvent(p.getId()));
        }
    }

    private boolean hasPendingPricing(Product p) {
        return pricingSuggestions.existsByProductIdAndStatus(p.getId(), SuggestionStatus.PENDING);
    }

    private Product find(Long id) {
        return products.findById(id).orElseThrow(() -> new NotFoundException("Product " + id + " not found"));
    }
}
