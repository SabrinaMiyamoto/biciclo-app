package com.biciclo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração da Overpass API (OpenStreetMap), bindada de app.overpass.*.
 *
 * @param url             endpoint do interpretador Overpass
 * @param bbox            área de busca no formato "sul,oeste,norte,leste"
 * @param timeoutSegundos tempo máximo de leitura da resposta
 */
@ConfigurationProperties(prefix = "app.overpass")
public record OverpassProperties(
        String url,
        String bbox,
        Integer timeoutSegundos
) {

    public OverpassProperties {
        if (url == null || url.isBlank()) {
            url = "https://overpass-api.de/api/interpreter";
        }
        if (bbox == null || bbox.isBlank()) {
            bbox = "-27.85,-48.62,-27.38,-48.35";
        }
        if (timeoutSegundos == null || timeoutSegundos <= 0) {
            timeoutSegundos = 60;
        }
    }
}
