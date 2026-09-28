package com.stockpulse.ai;

public interface LLMGateway {

    /**
     * Sends the prompt and returns the model's raw text.
     *
     * @throws LlmException on any transport, auth, quota or empty-response failure
     */
    String callLLM(String prompt);
}
