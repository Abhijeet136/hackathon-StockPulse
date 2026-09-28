package com.stockpulse.ai;

import com.fasterxml.jackson.databind.JsonNode;
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
 * Alternative gateway for OpenAI-compatible chat-completions APIs (Groq, OpenAI, Ollama...).
 * Enable with llm.provider=openai-compatible and set llm.base-url / LLM_MODEL / the key.
 */
@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "openai-compatible")
public class OpenAiCompatibleGateway implements LLMGateway {

    private final RestClient restClient;
    private final LlmProperties props;

    public OpenAiCompatibleGateway(@Qualifier("llmRestClient") RestClient restClient, LlmProperties props) {
        this.restClient = restClient;
        this.props = props;
    }

    @Override
    public String callLLM(String prompt) {
        if (!props.hasApiKey()) {
            throw new LlmException("LLM API key is not set");
        }
        Map<String, Object> body = Map.of(
                "model", props.model(),
                "temperature", props.temperatureOrDefault(),
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(Map.of("role", "user", "content", prompt)));
        try {
            JsonNode response = restClient.post()
                    .uri("/v1/chat/completions")
                    .header("Authorization", "Bearer " + props.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode content = response == null ? null : response.path("choices").path(0).path("message").path("content");
            if (content == null || content.asText().isBlank()) {
                throw new LlmException("LLM returned no content");
            }
            return content.asText();
        } catch (RestClientResponseException e) {
            throw new LlmException("LLM HTTP " + e.getStatusCode().value(), e);
        } catch (ResourceAccessException e) {
            throw new LlmException("LLM unreachable or timed out: " + e.getMessage(), e);
        }
    }
}
