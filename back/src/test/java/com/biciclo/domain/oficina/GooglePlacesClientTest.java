package com.biciclo.domain.oficina;

import com.biciclo.common.exception.BusinessException;
import com.biciclo.config.GooglePlacesProperties;
import com.biciclo.domain.oficina.dto.OficinaResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GooglePlacesClientTest {

    private static final String URL = "https://places.googleapis.com/v1/places:searchNearby";

    @Test
    void buscaOficinasEConverteResposta() {
        RestClient.Builder builder = RestClient.builder().baseUrl(URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GooglePlacesClient client = new GooglePlacesClient(builder.build(), props("chave-teste"));

        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Goog-Api-Key", "chave-teste"))
                .andExpect(header("X-Goog-FieldMask", GooglePlacesClient.FIELD_MASK))
                .andExpect(jsonPath("$.includedTypes[0]").value("bicycle_store"))
                .andExpect(jsonPath("$.locationRestriction.circle.radius").value(2000.0))
                .andRespond(withSuccess("""
                        { "places": [
                          { "id": "ChIJ1", "displayName": { "text": "Pedal Floripa" },
                            "formattedAddress": "Rua A, 10", "googleMapsUri": "https://maps.google.com/?cid=1",
                            "location": { "latitude": -27.59, "longitude": -48.55 },
                            "rating": 4.7, "userRatingCount": 120,
                            "currentOpeningHours": { "openNow": true } },
                          { "id": "ChIJ2", "displayName": { "text": "Sem horário" },
                            "location": { "latitude": -27.6, "longitude": -48.5 } },
                          { "id": "ChIJ3", "displayName": { "text": "Sem localização" } }
                        ] }
                        """, MediaType.APPLICATION_JSON));

        List<OficinaResponse> oficinas = client.buscarOficinas(-27.59, -48.55, 2000);

        server.verify();
        assertEquals(2, oficinas.size());
        OficinaResponse primeira = oficinas.get(0);
        assertEquals("ChIJ1", primeira.placeId());
        assertEquals("Pedal Floripa", primeira.nome());
        assertEquals(4.7, primeira.nota());
        assertEquals(120, primeira.totalAvaliacoes());
        assertTrue(primeira.abertoAgora());
        assertNull(oficinas.get(1).abertoAgora());
    }

    @Test
    void semChaveResponde503SemChamarOGoogle() {
        RestClient.Builder builder = RestClient.builder().baseUrl(URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GooglePlacesClient client = new GooglePlacesClient(builder.build(), props(""));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> client.buscarOficinas(-27.59, -48.55, 2000));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
        server.verify();
    }

    @Test
    void erroDoGoogleViraIndisponivel() {
        RestClient.Builder builder = RestClient.builder().baseUrl(URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GooglePlacesClient client = new GooglePlacesClient(builder.build(), props("chave-teste"));
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.FORBIDDEN));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> client.buscarOficinas(-27.59, -48.55, 2000));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
    }

    private static GooglePlacesProperties props(String chave) {
        return new GooglePlacesProperties(chave, URL, 5);
    }
}
