package com.stockpulse.config;

import com.stockpulse.ai.LlmProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Timeouts come from spring.http.client.connect-timeout / read-timeout via the auto-configured builder. */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient llmRestClient(RestClient.Builder builder, LlmProperties props) {
        return builder.baseUrl(props.baseUrl()).build();
    }
}
