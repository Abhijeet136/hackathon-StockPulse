package com.stockpulse.config;

import com.stockpulse.ai.LlmProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({LlmProperties.class, StockPulseProperties.class})
public class AppConfig {
}
