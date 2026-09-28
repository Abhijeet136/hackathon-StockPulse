package com.stockpulse.ai;

import com.stockpulse.domain.TriggerReason;
import com.stockpulse.strategy.PricingContext;
import com.stockpulse.strategy.ReorderContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Picks a prompt template by trigger reason and fills its {placeholders}. */
@Component
public class PromptBuilder {

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String pricingPrompt(PricingContext ctx) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("sku", ctx.sku());
        values.put("name", ctx.name());
        values.put("category", ctx.category().name());
        values.put("price", ctx.currentPrice().toPlainString());
        values.put("minPrice", ctx.currentPrice().multiply(new BigDecimal("0.5")).setScale(2, RoundingMode.UP).toPlainString());
        values.put("maxPrice", ctx.currentPrice().multiply(new BigDecimal("1.5")).setScale(2, RoundingMode.DOWN).toPlainString());
        values.put("stock", String.valueOf(ctx.stockLevel()));
        values.put("threshold", String.valueOf(ctx.reorderThreshold()));
        values.put("velocity", String.valueOf(ctx.demandVelocity()));
        values.put("avg", format(ctx.peerAverageVelocity()));
        values.put("multiplier", format(ctx.spikeMultiplier()));
        values.put("ratio", ctx.peerAverageVelocity() > 0 ? format(ctx.demandVelocity() / ctx.peerAverageVelocity()) : "n/a");
        values.put("daysOfStock", daysOfStock(ctx.stockLevel(), ctx.demandVelocity()));
        return fill("prompts/pricing-" + suffix(ctx.trigger()) + ".txt", values);
    }

    public String reorderPrompt(ReorderContext ctx) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("sku", ctx.sku());
        values.put("name", ctx.name());
        values.put("category", ctx.category().name());
        values.put("stock", String.valueOf(ctx.stockLevel()));
        values.put("threshold", String.valueOf(ctx.reorderThreshold()));
        values.put("velocity", String.valueOf(ctx.demandVelocity()));
        values.put("avg", format(ctx.peerAverageVelocity()));
        values.put("leadTime", String.valueOf(ctx.leadTimeDays()));
        values.put("daysOfStock", daysOfStock(ctx.stockLevel(), ctx.demandVelocity()));
        values.put("maxQty", String.valueOf(SuggestionValidator.maxReorderQuantity(ctx.reorderThreshold())));
        return fill("prompts/reorder-" + suffix(ctx.trigger()) + ".txt", values);
    }

    static String suffix(TriggerReason trigger) {
        return switch (trigger) {
            case INVENTORY_LOW -> "low-stock";
            case DEMAND_SPIKE -> "spike";
            case MANUAL, INITIAL -> "manual";
        };
    }

    private String fill(String path, Map<String, String> values) {
        String text = cache.computeIfAbsent(path, PromptBuilder::load);
        for (Map.Entry<String, String> e : values.entrySet()) {
            text = text.replace("{" + e.getKey() + "}", e.getValue());
        }
        return text;
    }

    private static String load(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Missing prompt template " + path, e);
        }
    }

    private static String daysOfStock(int stock, int velocity) {
        return velocity > 0 ? format((double) stock / velocity) : "unknown (no recent sales)";
    }

    private static String format(double d) {
        return String.format(java.util.Locale.ROOT, "%.1f", d);
    }
}
