package com.stockpulse.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Google Gemini via the native generateContent API. The key travels in the x-goog-api-key header, never the URL.
 */
@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "gemini", matchIfMissing = true)
public class GeminiGateway implements LLMGateway {

    private static final Logger log = LoggerFactory.getLogger(GeminiGateway.class);

    private final RestClient restClient;
    private final LlmProperties props;

    public GeminiGateway(@Qualifier("llmRestClient") RestClient restClient, LlmProperties props) {
        this.restClient = restClient;
        this.props = props;
    }

    @Override
    public String callLLM(String prompt) {
        if (!props.hasApiKey()) {
            throw new LlmException("GEMINI_API_KEY is not set");
        }
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "temperature", props.temperatureOrDefault(),
                        "responseMimeType", "application/json"));
        long start = System.currentTimeMillis();
        try {
            JsonNode response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent", props.model())
                    .header("x-goog-api-key", props.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String text = extractText(response);
            log.info("Gemini {} answered in {} ms", props.model(), System.currentTimeMillis() - start);
            return text;
        } catch (RestClientResponseException e) {
            throw new LlmException("Gemini HTTP " + e.getStatusCode().value() + ": " + abbreviate(e.getResponseBodyAsString()), e);
        } catch (ResourceAccessException e) {
            throw new LlmException("Gemini unreachable or timed out: " + e.getMessage(), e);
        }
    }

    private static String extractText(JsonNode response) {
        JsonNode text = response == null ? null
                : response.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (text == null || text.isMissingNode() || text.asText().isBlank()) {
            String reason = response == null ? "no body"
                    : response.path("candidates").path(0).path("finishReason").asText("no candidates");
            throw new LlmException("Gemini returned no text (" + reason + ")");
        }
        return text.asText();
    }

    private static String abbreviate(String s) {
        if (s == null) {
            return "";
        }
        String oneLine = s.replaceAll("\\s+", " ");
        return oneLine.length() > 300 ? oneLine.substring(0, 300) + "..." : oneLine;
    }
}
