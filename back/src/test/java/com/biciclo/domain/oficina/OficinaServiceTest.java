package com.biciclo.domain.oficina;

import com.biciclo.common.exception.BusinessException;
import com.biciclo.domain.oficina.dto.OficinaResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OficinaServiceTest {

    @Mock
    private GooglePlacesClient client;

    @InjectMocks
    private OficinaService service;

    @Test
    void repassaBuscaParaOGoogle() {
        OficinaResponse oficina = new OficinaResponse("p1", "Oficina", null, -27.59, -48.55,
                4.5, 10, true, null);
        when(client.buscarOficinas(-27.59, -48.55, 2000)).thenReturn(List.of(oficina));

        assertEquals(List.of(oficina), service.buscarProximas(-27.59, -48.55, 2000));
    }

    @Test
    void rejeitaRaioInvalidoSemGastarCota() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.buscarProximas(-27.59, -48.55, 6000));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertThrows(BusinessException.class, () -> service.buscarProximas(-27.59, -48.55, 0));
        assertThrows(BusinessException.class, () -> service.buscarProximas(95, -48.55, 1000));
        verifyNoInteractions(client);
    }
}
