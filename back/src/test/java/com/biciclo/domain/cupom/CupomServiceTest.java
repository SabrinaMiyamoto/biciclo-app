package com.biciclo.domain.cupom;

import com.biciclo.common.enums.StatusCupom;
import com.biciclo.common.exception.BusinessException;
import com.biciclo.common.exception.ResourceNotFoundException;
import com.biciclo.domain.cupom.dto.CupomResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CupomServiceTest {

    @Mock
    private CupomRepository repository;

    @InjectMocks
    private CupomService service;

    @Test
    void listaMeusCupons() {
        when(repository.findByCiclistaIdOrderByDataResgateDesc("u1"))
                .thenReturn(List.of(cupom(StatusCupom.PENDENTE)));

        List<CupomResponse> response = service.meus("u1");

        assertEquals(1, response.size());
        assertEquals("cupom-1", response.get(0).id());
        assertEquals(StatusCupom.PENDENTE, response.get(0).status());
    }

    @Test
    void validaCupomComSucesso() {
        when(repository.validar("CODIGO123", "p1"))
                .thenReturn(ValidacaoResultado.ok(cupom(StatusCupom.UTILIZADO)));

        CupomResponse response = service.validar("CODIGO123", "p1");

        assertEquals(StatusCupom.UTILIZADO, response.status());
        assertNotNull(response.dataUtilizacao());
    }

    @Test
    void cupomDeOutroParceiroRetorna403() {
        when(repository.validar("CODIGO123", "p1"))
                .thenReturn(ValidacaoResultado.erro(ValidacaoResultado.Status.SEM_PERMISSAO));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.validar("CODIGO123", "p1"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void cupomJaUtilizadoRetorna422() {
        when(repository.validar("CODIGO123", "p1"))
                .thenReturn(ValidacaoResultado.erro(ValidacaoResultado.Status.JA_UTILIZADO));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.validar("CODIGO123", "p1"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        assertEquals("Cupom já utilizado", ex.getMessage());
    }

    @Test
    void cupomNaoEncontradoRetorna404() {
        when(repository.validar("CODIGO123", "p1"))
                .thenReturn(ValidacaoResultado.erro(ValidacaoResultado.Status.NAO_ENCONTRADO));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.validar("CODIGO123", "p1"));

        assertEquals("Cupom não encontrado", ex.getMessage());
    }

    private static Cupom cupom(StatusCupom status) {
        Cupom cupom = new Cupom();
        cupom.setId("cupom-1");
        cupom.setCodigoValidacao("CODIGO123");
        cupom.setRecompensaId("r1");
        cupom.setCiclistaId("u1");
        cupom.setStatus(status);
        cupom.setDataResgate(new Date());
        if (status == StatusCupom.UTILIZADO) {
            cupom.setDataUtilizacao(new Date());
        }
        return cupom;
    }
}
