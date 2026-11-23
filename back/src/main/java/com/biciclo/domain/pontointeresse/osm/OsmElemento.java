package com.biciclo.domain.pontointeresse.osm;

import java.util.Map;

/**
 * Elemento do OpenStreetMap já simplificado (coordenada resolvida).
 * DTO simples, serializável no cache Redis.
 *
 * @param tipo "node", "way" ou "relation"
 */
public record OsmElemento(
        String tipo,
        long id,
        double latitude,
        double longitude,
        Map<String, String> tags
) {

    public String referencia() {
        return tipo + "/" + id;
    }
}
