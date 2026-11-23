package com.biciclo.domain.oficina;

import com.biciclo.common.exception.BusinessException;
import com.biciclo.config.GooglePlacesProperties;
import com.biciclo.domain.oficina.dto.OficinaResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Cliente do Nearby Search da Google Places API (New).
 *
 * Custo: cada chamada é cobrada pelo SKU dos campos pedidos no FieldMask.
 * nota, avaliações e horário colocam a chamada no SKU "Enterprise", que tem a
 * menor cota gratuita mensal. Por isso a rota exige login e não há busca
 * automática: o app só chama quando o ciclista pede oficinas.
 */
@Component
public class GooglePlacesClient {

    private static final Logger log = LoggerFactory.getLogger(GooglePlacesClient.class);

    static final String TIPO_OFICINA = "bicycle_store";
    static final int MAXIMO_RESULTADOS = 20;

    static final String FIELD_MASK = String.join(",",
            "places.id",
            "places.displayName",
            "places.formattedAddress",
            "places.location",
            "places.googleMapsUri",
            "places.rating",
            "places.userRatingCount",
            "places.currentOpeningHours.openNow");

    private final RestClient restClient;
    private final GooglePlacesProperties properties;

    @Autowired
    public GooglePlacesClient(RestClient.Builder builder, GooglePlacesProperties properties) {
        this(builder.baseUrl(properties.url()).requestFactory(requestFactory(properties)).build(), properties);
    }

    /** Usado nos testes, com um RestClient ligado ao MockRestServiceServer. */
    GooglePlacesClient(RestClient restClient, GooglePlacesProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    private static SimpleClientHttpRequestFactory requestFactory(GooglePlacesProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.timeoutSegundos()));
        return requestFactory;
    }

    public boolean configurado() {
        return properties.configurada();
    }

    public List<OficinaResponse> buscarOficinas(double latitude, double longitude, double raioMetros) {
        if (!properties.configurada()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Busca de oficinas no Google não configurada (GOOGLE_PLACES_API_KEY)");
        }

        Map<String, Object> corpo = Map.of(
                "includedTypes", List.of(TIPO_OFICINA),
                "maxResultCount", MAXIMO_RESULTADOS,
                "rankPreference", "DISTANCE",
                "languageCode", "pt-BR",
                "locationRestriction", Map.of("circle", Map.of(
                        "center", Map.of("latitude", latitude, "longitude", longitude),
                        "radius", raioMetros)));

        RespostaPlaces resposta;
        try {
            resposta = restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Goog-Api-Key", properties.apiKey())
                    .header("X-Goog-FieldMask", FIELD_MASK)
                    .body(corpo)
                    .retrieve()
                    .body(RespostaPlaces.class);
        } catch (RestClientException e) {
            // Não loga a URL nem os headers para não vazar a chave.
            log.warn("Falha ao consultar o Google Places: {}", e.getClass().getSimpleName());
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Google Places indisponível. Tente novamente mais tarde.");
        }

        if (resposta == null || resposta.places() == null) {
            return List.of();
        }
        return resposta.places().stream()
                .filter(place -> place.location() != null)
                .map(GooglePlacesClient::converter)
                .toList();
    }

    static OficinaResponse converter(Place place) {
        return new OficinaResponse(
                place.id(),
                place.displayName() == null ? null : place.displayName().text(),
                place.formattedAddress(),
                place.location().latitude(),
                place.location().longitude(),
                place.rating(),
                place.userRatingCount(),
                place.currentOpeningHours() == null ? null : place.currentOpeningHours().openNow(),
                place.googleMapsUri());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespostaPlaces(List<Place> places) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Place(String id, Texto displayName, String formattedAddress, Localizacao location,
                 String googleMapsUri, Double rating, Integer userRatingCount, Horario currentOpeningHours) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Texto(String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Localizacao(Double latitude, Double longitude) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Horario(Boolean openNow) {
    }
}
