package com.biciclo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Origens liberadas no CORS, bindadas a partir de app.cors.* no application.yml.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        List<String> allowedOrigins
) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null
                ? List.of()
                : allowedOrigins.stream()
                        .map(String::trim)
                        .filter(origem -> !origem.isEmpty())
                        .toList();
        if (allowedOrigins.contains("*")) {
            throw new IllegalStateException(
                    "app.cors.allowed-origins não pode conter '*'. Liste as origens do front-end.");
        }
    }
}
