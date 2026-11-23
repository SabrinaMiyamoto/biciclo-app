package com.biciclo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades do Firebase, bindadas a partir de app.firebase.* no application.yml.
 */
@ConfigurationProperties(prefix = "app.firebase")
public record FirebaseProperties(
        String serviceAccountPath
) {
}
