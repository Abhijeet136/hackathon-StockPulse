package com.stockpulse.service;

import com.stockpulse.config.StockPulseProperties;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.TriggerReason;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.strategy.PricingContext;
import com.stockpulse.strategy.ReorderContext;
import org.springframework.stereotype.Component;

/** Turns a Product entity into the plain records strategies work with. */
@Component
public class ContextFactory {

    private final ProductRepository products;
    private final StockPulseProperties props;

    public ContextFactory(ProductRepository products, StockPulseProperties props) {
        this.products = products;
        this.props = props;
    }

    public double peerAverageVelocity(Product p) {
        return products.averageVelocityOfPeers(p.getCategory(), p.getId()).orElse(0.0);
    }

    public double spikeMultiplier() {
        return props.spike().multiplier();
    }

    /** Spike = velocity above multiplier x peers' average (the product itself is excluded from the average). */
    public boolean isSpiking(Product p) {
        return pricing(p, TriggerReason.DEMAND_SPIKE).isSpiking();
    }

    public PricingContext pricing(Product p, TriggerReason trigger) {
        return new PricingContext(p.getSku(), p.getName(), p.getCategory(), p.getCurrentPrice(),
                p.getStockLevel(), p.getReorderThreshold(), p.getDemandVelocity(),
                peerAverageVelocity(p), spikeMultiplier(), trigger);
    }

    public ReorderContext reorder(Product p, TriggerReason trigger) {
        return new ReorderContext(p.getSku(), p.getName(), p.getCategory(), p.getStockLevel(),
                p.getReorderThreshold(), p.getDemandVelocity(), peerAverageVelocity(p),
                props.reorder().leadTimeDays(), trigger);
    }
}
