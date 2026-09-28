package com.stockpulse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("stockpulse")
public record StockPulseProperties(Spike spike, Reorder reorder, Cors cors) {

    public record Spike(double multiplier) {
    }

    public record Reorder(int leadTimeDays) {
    }

    public record Cors(List<String> allowedOrigins) {
    }
}
