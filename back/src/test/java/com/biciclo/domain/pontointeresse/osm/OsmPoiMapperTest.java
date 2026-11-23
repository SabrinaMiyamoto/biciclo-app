package com.biciclo.domain.pontointeresse.osm;

import com.biciclo.common.enums.TipoPoi;
import com.biciclo.domain.pontointeresse.PontoInteresse;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OsmPoiMapperTest {

    @Test
    void converteBicicletarioComCapacidade() {
        OsmElemento elemento = new OsmElemento("node", 123, -27.59, -48.55,
                new HashMap<>(Map.of("amenity", "bicycle_parking", "capacity", "12")));

        PontoInteresse poi = OsmPoiMapper.paraPoi(elemento).orElseThrow();

        assertEquals("osm-node-123", poi.getId());
        assertEquals(TipoPoi.BICICLETARIO, poi.getTipo());
        assertEquals("Bicicletário", poi.getDescricao());
        assertEquals(12, poi.getCapacidade());
        assertEquals("OSM", poi.getOrigem());
        assertEquals("node/123", poi.getOsmId());
        assertEquals(-27.59, poi.getLocalizacao().getLatitude());
    }

    @Test
    void usaNomeDoOsmQuandoExiste() {
        OsmElemento elemento = new OsmElemento("way", 9, -27.6, -48.5,
                Map.of("shop", "bicycle", "name", "Pedal Floripa"));

        PontoInteresse poi = OsmPoiMapper.paraPoi(elemento).orElseThrow();

        assertEquals(TipoPoi.OFICINA, poi.getTipo());
        assertEquals("Pedal Floripa", poi.getDescricao());
        assertEquals("osm-way-9", poi.getId());
    }

    @Test
    void mapeiaTiposConhecidos() {
        assertEquals(TipoPoi.PONTO_APOIO,
                OsmPoiMapper.tipo(Map.of("amenity", "bicycle_repair_station")).orElseThrow());
        assertEquals(TipoPoi.PONTO_APOIO,
                OsmPoiMapper.tipo(Map.of("amenity", "bicycle_rental")).orElseThrow());
        assertEquals(TipoPoi.TERMINAL,
                OsmPoiMapper.tipo(Map.of("amenity", "bus_station")).orElseThrow());
    }

    @Test
    void ignoraTipoDesconhecido() {
        OsmElemento elemento = new OsmElemento("node", 1, -27.6, -48.5, Map.of("amenity", "bench"));

        assertTrue(OsmPoiMapper.paraPoi(elemento).isEmpty());
    }

    @Test
    void capacidadeInvalidaViraNula() {
        assertNull(OsmPoiMapper.capacidade("muitas"));
        assertNull(OsmPoiMapper.capacidade("-3"));
        assertEquals(4, OsmPoiMapper.capacidade(" 4 "));
    }
}
