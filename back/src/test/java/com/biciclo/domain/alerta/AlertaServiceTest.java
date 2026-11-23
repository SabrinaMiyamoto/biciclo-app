package com.biciclo.domain.alerta;

import com.biciclo.common.enums.StatusAlerta;
import com.biciclo.common.exception.BusinessException;
import com.biciclo.common.exception.ResourceNotFoundException;
import com.biciclo.domain.alerta.dto.AlertaRequest;
import com.biciclo.domain.alerta.dto.AlertaResponse;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertaServiceTest {

    @Mock
    private AlertaRepository repository;

    @InjectMocks
    private AlertaService service;

    @Test
    void registraAlertaAberto() {
        when(repository.save(any(AlertaInfraestrutura.class))).thenAnswer(invocation -> {
            AlertaInfraestrutura alerta = invocation.getArgument(0);
            alerta.setId("a1");
            return alerta;
        });

        AlertaResponse response = service.registrar("u1", request("BURACO", -27.59, -48.55));

        ArgumentCaptor<AlertaInfraestrutura> captor =
                ArgumentCaptor.forClass(AlertaInfraestrutura.class);
        verify(repository).save(captor.capture());

        assertEquals("u1", captor.getValue().getCiclistaId());
        assertEquals(StatusAlerta.ABERTO, captor.getValue().getStatus());
        assertEquals("a1", response.id());
        assertEquals(-27.59, response.latitude());
    }

    @Test
    void rejeitaCoordenadasInvalidas() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.registrar("u1", request("BURACO", 100.0, -48.55)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void moderaAlertaAbertoParaEmAnalise() {
        AlertaInfraestrutura alerta = alerta(StatusAlerta.ABERTO);
        when(repository.findById("a1")).thenReturn(Optional.of(alerta));
        when(repository.save(alerta)).thenReturn(alerta);

        AlertaResponse response = service.atualizarStatus("a1", StatusAlerta.EM_ANALISE, "admin1");

        assertEquals(StatusAlerta.EM_ANALISE, response.status());
        assertEquals("admin1", alerta.getModeradorId());
        assertNotNull(response.updatedAt());
    }

    @Test
    void naoReabreAlertaResolvido() {
        when(repository.findById("a1")).thenReturn(Optional.of(alerta(StatusAlerta.RESOLVIDO)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.atualizarStatus("a1", StatusAlerta.ABERTO, "admin1"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void moderarAlertaInexistenteDa404() {
        when(repository.findById("x")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.atualizarStatus("x", StatusAlerta.RESOLVIDO, "admin1"));
    }

    @Test
    void listaFilaDeModeracaoPorStatus() {
        when(repository.findByStatus(StatusAlerta.ABERTO, 100)).thenReturn(List.of(alerta(StatusAlerta.ABERTO)));

        List<AlertaResponse> fila = service.listar(StatusAlerta.ABERTO);

        assertEquals(1, fila.size());
        assertEquals("a1", fila.get(0).id());
    }

    @Test
    void listaAlertasDoCiclista() {
        when(repository.findByCiclistaId("u1", 100)).thenReturn(List.of(alerta(StatusAlerta.ABERTO)));

        assertEquals(1, service.minhas("u1").size());
    }

    private static AlertaInfraestrutura alerta(StatusAlerta status) {
        AlertaInfraestrutura alerta = new AlertaInfraestrutura();
        alerta.setId("a1");
        alerta.setCiclistaId("u1");
        alerta.setTipo("BURACO");
        alerta.setDescricao("Buraco na ciclovia");
        alerta.setStatus(status);
        return alerta;
    }

    private static AlertaRequest request(String tipo, double latitude, double longitude) {
        AlertaRequest request = new AlertaRequest();
        request.setTipo(tipo);
        request.setDescricao("Descrição do problema");
        request.setLatitude(latitude);
        request.setLongitude(longitude);
        return request;
    }
}
