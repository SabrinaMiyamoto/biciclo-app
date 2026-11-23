package com.biciclo.domain.pontointeresse.osm;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Garante que o valor guardado no Redis volta igual (RNF04). */
class OsmElementoCacheTest {

    @Test
    void serializaEDesserializaListaNoFormatoDoCache() {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        List<OsmElemento> original = new ArrayList<>();
        original.add(new OsmElemento("node", 1, -27.59, -48.55,
                new HashMap<>(Map.of("amenity", "bicycle_parking"))));

        Object lido = serializer.deserialize(serializer.serialize(original));

        assertEquals(original, lido);
    }
}
