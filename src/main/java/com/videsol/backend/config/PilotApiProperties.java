package com.videsol.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pilot.api")
public record PilotApiProperties(
        String baseUrl,
        String username,
        String password,
        int tokenTtlMinutes,
        int connectTimeoutMs,
        int readTimeoutMs
) {}
