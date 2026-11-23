package com.biciclo.domain.pontointeresse;

import com.biciclo.common.enums.TipoPoi;
import com.biciclo.common.exception.BusinessException;
import com.biciclo.common.exception.ResourceNotFoundException;
import com.biciclo.domain.pontointeresse.dto.PoiRequest;
import com.biciclo.domain.pontointeresse.dto.PoiResponse;
import com.google.cloud.firestore.GeoPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PontoInteresseServiceTest {

    @Mock
    private PontoInteresseRepository repository;

    @InjectMocks
    private PontoInteresseService service;

    @Test
    void buscaProximosRepassaRaioELimiteEConverteParaDto() {
        when(repository.buscarProximos(-27.59, -48.55, 1000, 50))
                .thenReturn(List.of(poi("p1", TipoPoi.BICICLETARIO, -27.591, -48.551)));

        List<PoiResponse> resultado = service.buscarProximos(-27.59, -48.55, 1000);

        assertEquals(1, resultado.size());
        assertEquals("p1", resultado.get(0).id());
        assertEquals(TipoPoi.BICICLETARIO, resultado.get(0).tipo());
        assertEquals(-27.591, resultado.get(0).latitude());
        assertEquals(-48.551, resultado.get(0).longitude());
    }

    @Test
    void aceitaRaioMaximoDeCincoQuilometros() {
        when(repository.buscarProximos(anyDouble(), anyDouble(), eq(5000.0), anyInt())).thenReturn(List.of());

        assertTrue(service.buscarProximos(-27.59, -48.55, 5000).isEmpty());
    }

    @Test
    void rejeitaRaioAcimaDeCincoQuilometros() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.buscarProximos(-27.59, -48.55, 5001));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verifyNoInteractions(repository);
    }

    @Test
    void rejeitaRaioZeroOuNegativo() {
        assertThrows(BusinessException.class, () -> service.buscarProximos(-27.59, -48.55, 0));
        assertThrows(BusinessException.class, () -> service.buscarProximos(-27.59, -48.55, -10));
        verifyNoInteractions(repository);
    }

    @Test
    void rejeitaCoordenadasInvalidas() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.buscarProximos(-91, -48.55, 1000));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verifyNoInteractions(repository);
    }

    @Test
    void criaPoiManualAtivo() {
        when(repository.save(any(PontoInteresse.class))).thenAnswer(invocation -> {
            PontoInteresse poi = invocation.getArgument(0);
            poi.setId("novo");
            return poi;
        });

        PoiResponse response = service.criar(request(TipoPoi.OFICINA, "  Oficina do Zé  "));

        ArgumentCaptor<PontoInteresse> captor = ArgumentCaptor.forClass(PontoInteresse.class);
        verify(repository).save(captor.capture());
        assertEquals("MANUAL", captor.getValue().getOrigem());
        assertEquals("Oficina do Zé", captor.getValue().getDescricao());
        assertTrue(captor.getValue().getAtivo());
        assertEquals("novo", response.id());
    }

    @Test
    void atualizaPoiExistenteMantendoOrigem() {
        PontoInteresse existente = poi("p1", TipoPoi.BICICLETARIO, -27.5, -48.5);
        existente.setOrigem("OSM");
        when(repository.findById("p1")).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);

        PoiResponse response = service.atualizar("p1", request(TipoPoi.PONTO_APOIO, "Novo nome"));

        assertEquals(TipoPoi.PONTO_APOIO, response.tipo());
        assertEquals("Novo nome", response.descricao());
        assertEquals("OSM", response.origem());
    }

    @Test
    void desativaPoi() {
        PontoInteresse existente = poi("p1", TipoPoi.BICICLETARIO, -27.5, -48.5);
        when(repository.findById("p1")).thenReturn(Optional.of(existente));

        service.desativar("p1");

        assertFalse(existente.getAtivo());
        verify(repository).save(existente);
    }

    @Test
    void falhaAoEditarPoiInexistente() {
        when(repository.findById("x")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.atualizar("x", request(TipoPoi.OFICINA, "Oficina")));
        verify(repository, never()).save(any());
    }

    private static PontoInteresse poi(String id, TipoPoi tipo, double lat, double lng) {
        PontoInteresse poi = new PontoInteresse();
        poi.setId(id);
        poi.setTipo(tipo);
        poi.setDescricao("POI " + id);
        poi.setLocalizacao(new GeoPoint(lat, lng));
        return poi;
    }

    private static PoiRequest request(TipoPoi tipo, String descricao) {
        PoiRequest request = new PoiRequest();
        request.setTipo(tipo);
        request.setDescricao(descricao);
        request.setLatitude(-27.59);
        request.setLongitude(-48.55);
        request.setCapacidade(10);
        return request;
    }
}
