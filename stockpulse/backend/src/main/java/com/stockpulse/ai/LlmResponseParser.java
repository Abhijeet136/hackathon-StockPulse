package com.stockpulse.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.stereotype.Component;

/** Defensive parsing: strip ``` fences, take the outermost {...}, ignore unknown fields. */
@Component
public class LlmResponseParser {

    private final ObjectMapper mapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .build();

    public LlmPricingResponse parsePricing(String raw) {
        return parse(raw, LlmPricingResponse.class);
    }

    public LlmReorderResponse parseReorder(String raw) {
        return parse(raw, LlmReorderResponse.class);
    }

    <T> T parse(String raw, Class<T> type) {
        String json = extractJson(raw);
        try {
            return mapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new LlmException("Unparseable LLM JSON: " + e.getOriginalMessage());
        }
    }

    static String extractJson(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new LlmException("Empty LLM response");
        }
        String text = raw.replaceAll("```(?:json)?", "");
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new LlmException("No JSON object in LLM response");
        }
        return text.substring(start, end + 1);
    }
}
