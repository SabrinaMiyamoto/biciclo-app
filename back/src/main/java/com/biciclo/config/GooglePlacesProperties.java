package com.biciclo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Google Places API (New), bindada de app.google-places.*.
 *
 * @param apiKey          chave do Google Cloud (GOOGLE_PLACES_API_KEY). Vazia desliga a busca.
 * @param url             endpoint do Nearby Search
 * @param timeoutSegundos tempo máximo de leitura da resposta
 */
@ConfigurationProperties(prefix = "app.google-places")
public record GooglePlacesProperties(
        String apiKey,
        String url,
        Integer timeoutSegundos
) {

    public GooglePlacesProperties {
        if (url == null || url.isBlank()) {
            url = "https://places.googleapis.com/v1/places:searchNearby";
        }
        if (timeoutSegundos == null || timeoutSegundos <= 0) {
            timeoutSegundos = 10;
        }
    }

    public boolean configurada() {
        return apiKey != null && !apiKey.isBlank();
    }
}
