package com.biciclo.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void regraDeNegocioMantemStatusConfigurado() {
        ResponseEntity<ApiError> response =
                handler.handleBusiness(new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "Saldo insuficiente"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Saldo insuficiente", response.getBody().message());
    }

    @Test
    void recursoNaoEncontradoRetorna404() {
        ResponseEntity<ApiError> response =
                handler.handleNotFound(new ResourceNotFoundException("Trajeto não encontrado"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Trajeto não encontrado", response.getBody().message());
    }

    @Test
    void parametroObrigatorioAusenteRetorna400() {
        ResponseEntity<ApiError> response = handler.handleMissingParameter(
                new MissingServletRequestParameterException("latitude", "double"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Parâmetro 'latitude' é obrigatório", response.getBody().message());
    }

    @Test
    void payloadMalformadoRetorna400() {
        ResponseEntity<ApiError> response = handler.handleUnreadable(
                new HttpMessageNotReadableException("JSON inválido"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Payload inválido ou malformado", response.getBody().message());
    }

    @Test
    void erroInesperadoRetorna500SemVazarDetalhes() {
        ResponseEntity<ApiError> response = handler.handleGeneric(new RuntimeException("stack trace interno"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Erro interno do servidor", response.getBody().message());
    }
}
