package com.biciclo.domain.pontointeresse.osm;

import com.biciclo.common.exception.BusinessException;
import com.biciclo.config.OverpassProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente da Overpass API (RF06). O resultado fica 24 h no Redis (RNF04),
 * então a busca continua funcionando quando a Overpass estiver fora do ar.
 */
@Component
public class OverpassClient {

    public static final String CACHE = "osm-infraestrutura";

    private static final Logger log = LoggerFactory.getLogger(OverpassClient.class);

    /**
     * Infraestrutura cicloviária: bicicletários, oficinas, estações de reparo,
     * aluguel de bicicletas e terminais de ônibus (integração modal).
     * "out center" devolve o centro de ways/relations.
     */
    private static final String QUERY = """
            [out:json][timeout:%d];
            (
              nwr["amenity"="bicycle_parking"](%s);
              nwr["shop"="bicycle"](%s);
              nwr["amenity"="bicycle_repair_station"](%s);
              nwr["amenity"="bicycle_rental"](%s);
              nwr["amenity"="bus_station"](%s);
            );
            out center tags;
            """;

    private final RestClient restClient;
    private final OverpassProperties properties;

    public OverpassClient(RestClient.Builder builder, OverpassProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.timeoutSegundos()));
        this.restClient = builder
                .baseUrl(properties.url())
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", "biciclo-api (projeto integrador)")
                .build();
        this.properties = properties;
    }

    /** Usa o cache quando houver; senão consulta a Overpass. */
    @Cacheable(cacheNames = CACHE, key = "'bbox:' + #root.target.bbox()")
    public List<OsmElemento> buscarInfraestrutura() {
        return consultar();
    }

    /** Ignora o cache, consulta a Overpass e atualiza o cache. */
    @CachePut(cacheNames = CACHE, key = "'bbox:' + #root.target.bbox()")
    public List<OsmElemento> atualizarInfraestrutura() {
        return consultar();
    }

    public String bbox() {
        return properties.bbox();
    }

    private List<OsmElemento> consultar() {
        String bbox = properties.bbox();
        String query = QUERY.formatted(properties.timeoutSegundos(), bbox, bbox, bbox, bbox, bbox);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("data", query);

        RespostaOverpass resposta;
        try {
            resposta = restClient.post()
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(RespostaOverpass.class);
        } catch (RestClientException e) {
            log.warn("Falha ao consultar a Overpass API: {}", e.getMessage());
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                    "OpenStreetMap (Overpass API) indisponível. Tente novamente mais tarde.");
        }

        // ArrayList/HashMap (e não List.of/toList) para desserializar do Redis.
        List<OsmElemento> elementos = new ArrayList<>();
        if (resposta == null || resposta.elements() == null) {
            return elementos;
        }
        for (ElementoOverpass e : resposta.elements()) {
            Double lat = e.lat() != null ? e.lat() : e.center() == null ? null : e.center().lat();
            Double lon = e.lon() != null ? e.lon() : e.center() == null ? null : e.center().lon();
            if (lat == null || lon == null) {
                continue;
            }
            Map<String, String> tags = e.tags() == null ? new HashMap<>() : new HashMap<>(e.tags());
            elementos.add(new OsmElemento(e.type(), e.id(), lat, lon, tags));
        }
        log.info("Overpass retornou {} elementos para bbox {}", elementos.size(), bbox);
        return elementos;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespostaOverpass(List<ElementoOverpass> elements) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ElementoOverpass(String type, long id, Double lat, Double lon,
                            Centro center, Map<String, String> tags) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Centro(Double lat, Double lon) {
    }
}
