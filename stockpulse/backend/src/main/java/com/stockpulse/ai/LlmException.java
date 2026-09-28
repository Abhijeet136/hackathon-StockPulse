package com.stockpulse.ai;

/** Any LLM failure. Never reaches the HTTP client; it triggers the rule-based fallback. */
public class LlmException extends RuntimeException {
    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
