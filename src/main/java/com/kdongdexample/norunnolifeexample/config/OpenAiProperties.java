package com.kdongdexample.norunnolifeexample.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(
        String apiKey,
        String model,
        String baseUrl,
        int timeoutSeconds,
        String reasoningEffort,
        int maxOutputTokens
) {}
