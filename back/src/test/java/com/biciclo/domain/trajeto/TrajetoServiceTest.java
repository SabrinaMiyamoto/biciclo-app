package com.biciclo.domain.trajeto;

import com.biciclo.common.enums.StatusAnalise;
import com.biciclo.common.exception.BusinessException;
import com.biciclo.domain.trajeto.dto.PontoTrajetoRequest;
import com.biciclo.domain.trajeto.dto.TrajetoRequest;
import com.biciclo.domain.trajeto.dto.TrajetoResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrajetoServiceTest {

    private static final String CICLISTA = "uid-1";

    @Mock
    private TrajetoRepository repository;

    @InjectMocks
    private TrajetoService service;

    @Test
    void aprovaTrajetoValidoECalculaPontos() {
        Instant fim = Instant.now().minusSeconds(60);
        Instant inicio = fim.minusSeconds(600);

        TrajetoResponse response = service.registrar(CICLISTA, request(
                ponto(-27.5900, -48.5500, inicio),
                ponto(-27.6000, -48.5500, fim)));

        assertEquals(StatusAnalise.APROVADO, response.statusAnalise());
        assertEquals(11, response.pontosGanhos());
        assertEquals(600, response.duracaoSegundos());
        assertEquals(1111.9, response.distanciaMetros(), 2.0);
        assertEquals(6.67, response.velocidadeMedia(), 0.05);

        verify(repository).registrarAprovado(any());
        verify(repository, never()).save(any());
    }

    @Test
    void rejeitaTrajetoCurtoSemPontuar() {
        Instant fim = Instant.now().minusSeconds(60);
        Instant inicio = fim.minusSeconds(600);

        TrajetoResponse response = service.registrar(CICLISTA, request(
                ponto(-27.5900, -48.5500, inicio),
                ponto(-27.59005, -48.5500, fim)));

        assertEquals(StatusAnalise.REJEITADO, response.statusAnalise());
        assertEquals(0, response.pontosGanhos());

        verify(repository).save(any());
        verify(repository, never()).registrarAprovado(any());
    }

    @Test
    void rejeitaVelocidadeIncompativelComBicicleta() {
        Instant fim = Instant.now().minusSeconds(60);
        Instant inicio = fim.minusSeconds(30);

        TrajetoResponse response = service.registrar(CICLISTA, request(
                ponto(-27.5900, -48.5500, inicio),
                ponto(-27.6000, -48.5500, fim)));

        assertEquals(StatusAnalise.REJEITADO, response.statusAnalise());
        assertEquals(0, response.pontosGanhos());
        assertTrue(response.velocidadeMedia() > 60);
        verify(repository, never()).registrarAprovado(any());
    }

    @Test
    void rejeitaTimestampsForaDeOrdem() {
        Instant agora = Instant.now().minusSeconds(60);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.registrar(CICLISTA, request(
                        ponto(-27.5900, -48.5500, agora),
                        ponto(-27.6000, -48.5500, agora.minusSeconds(600)))));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(repository, never()).registrarAprovado(any());
        verify(repository, never()).save(any());
    }

    @Test
    void rejeitaDuracaoZero() {
        Instant agora = Instant.now().minusSeconds(60);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.registrar(CICLISTA, request(
                        ponto(-27.5900, -48.5500, agora),
                        ponto(-27.6000, -48.5500, agora))));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void rejeitaTrajetoComDataNoFuturo() {
        Instant fim = Instant.now().plusSeconds(3600);
        Instant inicio = fim.minusSeconds(600);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.registrar(CICLISTA, request(
                        ponto(-27.5900, -48.5500, inicio),
                        ponto(-27.6000, -48.5500, fim))));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void rejeitaCoordenadaForaDoIntervalo() {
        Instant fim = Instant.now().minusSeconds(60);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.registrar(CICLISTA, request(
                        ponto(100.0, -48.5500, fim.minusSeconds(600)),
                        ponto(-27.6000, -48.5500, fim))));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void listaTrajetosDoCiclista() {
        Trajeto trajeto = new Trajeto();
        trajeto.setId("trajeto-1");
        trajeto.setCiclistaId(CICLISTA);
        trajeto.setDistanciaMetros(1200.0);
        trajeto.setDuracaoSegundos(600);
        trajeto.setVelocidadeMedia(7.2);
        trajeto.setPontosGanhos(12);
        trajeto.setStatusAnalise(StatusAnalise.APROVADO);
        trajeto.setCreatedAt(new Date());

        when(repository.findByCiclistaIdOrderByCreatedAtDesc(CICLISTA))
                .thenReturn(List.of(trajeto));

        List<TrajetoResponse> response = service.listarDoCiclista(CICLISTA);

        assertEquals(1, response.size());
        assertEquals("trajeto-1", response.get(0).id());
        assertEquals(12, response.get(0).pontosGanhos());
        assertEquals(StatusAnalise.APROVADO, response.get(0).statusAnalise());
    }

    private static TrajetoRequest request(PontoTrajetoRequest... pontos) {
        TrajetoRequest request = new TrajetoRequest();
        request.setPontos(List.of(pontos));
        return request;
    }

    private static PontoTrajetoRequest ponto(double latitude, double longitude, Instant timestamp) {
        return new PontoTrajetoRequest(latitude, longitude, timestamp);
    }
}
