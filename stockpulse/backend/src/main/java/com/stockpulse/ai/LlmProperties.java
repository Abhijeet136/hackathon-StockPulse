package com.stockpulse.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * LLM settings. The API key comes only from the environment (GEMINI_API_KEY) and is never logged.
 */
@ConfigurationProperties("llm")
public record LlmProperties(String provider, String baseUrl, String model, String apiKey, Double temperature) {

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    public double temperatureOrDefault() {
        return temperature == null ? 0.3 : temperature;
    }

    @Override
    public String toString() {
        return "LlmProperties[provider=%s, baseUrl=%s, model=%s, apiKey=%s]"
                .formatted(provider, baseUrl, model, hasApiKey() ? "***" : "<missing>");
    }
}
