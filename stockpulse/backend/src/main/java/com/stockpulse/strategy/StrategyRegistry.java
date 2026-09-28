package com.stockpulse.strategy;

import com.stockpulse.exception.UnknownStrategyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds every strategy bean by short name ("ai", "ruleBased") and the active one per type.
 * Bean names follow the convention pricingXxx / reorderXxx; the prefix is stripped for the short name.
 */
@Component
public class StrategyRegistry {

    private static final Logger log = LoggerFactory.getLogger(StrategyRegistry.class);

    private final Map<String, PricingStrategy> pricing;
    private final Map<String, ReorderStrategy> reorder;
    private final AtomicReference<String> activePricing = new AtomicReference<>();
    private final AtomicReference<String> activeReorder = new AtomicReference<>();

    public StrategyRegistry(Map<String, PricingStrategy> pricingBeans,
                            Map<String, ReorderStrategy> reorderBeans,
                            @Value("${stockpulse.strategy.pricing:ruleBased}") String defaultPricing,
                            @Value("${stockpulse.strategy.reorder:ruleBased}") String defaultReorder) {
        this.pricing = shorten(pricingBeans, "pricing");
        this.reorder = shorten(reorderBeans, "reorder");
        switchPricing(defaultPricing);
        switchReorder(defaultReorder);
    }

    public PricingStrategy activePricing() {
        return pricing.get(activePricing.get());
    }

    public ReorderStrategy activeReorder() {
        return reorder.get(activeReorder.get());
    }

    public String activePricingName() {
        return activePricing.get();
    }

    public String activeReorderName() {
        return activeReorder.get();
    }

    public Set<String> pricingNames() {
        return pricing.keySet();
    }

    public Set<String> reorderNames() {
        return reorder.keySet();
    }

    public void switchPricing(String name) {
        requireKnown(pricing, name, "pricing");
        String previous = activePricing.getAndSet(name);
        log.info("Pricing strategy: {} -> {}", previous, name);
    }

    public void switchReorder(String name) {
        requireKnown(reorder, name, "reorder");
        String previous = activeReorder.getAndSet(name);
        log.info("Reorder strategy: {} -> {}", previous, name);
    }

    private static void requireKnown(Map<String, ?> map, String name, String type) {
        if (name == null || !map.containsKey(name)) {
            throw new UnknownStrategyException(
                    "Unknown %s strategy '%s'. Available: %s".formatted(type, name, map.keySet()));
        }
    }

    private static <T> Map<String, T> shorten(Map<String, T> beans, String prefix) {
        Map<String, T> result = new TreeMap<>();
        beans.forEach((beanName, bean) -> {
            String shortName = beanName.startsWith(prefix) && beanName.length() > prefix.length()
                    ? Character.toLowerCase(beanName.charAt(prefix.length())) + beanName.substring(prefix.length() + 1)
                    : beanName;
            result.put(shortName, bean);
        });
        return Collections.unmodifiableMap(result);
    }
}
