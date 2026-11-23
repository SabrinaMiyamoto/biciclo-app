package com.biciclo.domain.alerta;

import com.biciclo.common.enums.StatusAlerta;
import com.biciclo.common.exception.BusinessException;
import com.biciclo.common.exception.ResourceNotFoundException;
import com.biciclo.domain.alerta.dto.AlertaRequest;
import com.biciclo.domain.alerta.dto.AlertaResponse;
import com.google.cloud.firestore.GeoPoint;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class AlertaService {

    private static final int LIMITE_LISTAGEM = 100;

    private final AlertaRepository repository;

    public AlertaService(AlertaRepository repository) {
        this.repository = repository;
    }

    public AlertaResponse registrar(String ciclistaId, AlertaRequest request) {
        if (request.getLatitude() < -90 || request.getLatitude() > 90
                || request.getLongitude() < -180 || request.getLongitude() > 180) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Coordenadas inválidas");
        }

        AlertaInfraestrutura alerta = new AlertaInfraestrutura();
        alerta.setCiclistaId(ciclistaId);
        alerta.setTipo(request.getTipo().trim());
        alerta.setDescricao(request.getDescricao().trim());
        alerta.setLocalizacao(new GeoPoint(request.getLatitude(), request.getLongitude()));
        alerta.setFotoUrl(request.getFotoUrl());
        alerta.setStatus(StatusAlerta.ABERTO);
        alerta.setCreatedAt(new Date());

        return AlertaResponse.from(repository.save(alerta));
    }

    /** Alertas do próprio ciclista, mais recentes primeiro. */
    public List<AlertaResponse> minhas(String ciclistaId) {
        return repository.findByCiclistaId(ciclistaId, LIMITE_LISTAGEM).stream()
                .map(AlertaResponse::from)
                .toList();
    }

    /** Fila de moderação (ADMIN). Sem status, lista todos. */
    public List<AlertaResponse> listar(StatusAlerta status) {
        return repository.findByStatus(status, LIMITE_LISTAGEM).stream()
                .map(AlertaResponse::from)
                .toList();
    }

    /**
     * Moderação: ABERTO -> EM_ANALISE -> RESOLVIDO, com atalho ABERTO -> RESOLVIDO
     * e retorno EM_ANALISE -> ABERTO.
     * Um alerta resolvido não volta a ser aberto; o ciclista registra outro.
     */
    public AlertaResponse atualizarStatus(String id, StatusAlerta novoStatus, String moderadorId) {
        AlertaInfraestrutura alerta = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alerta não encontrado"));

        if (!transicaoPermitida(alerta.getStatus(), novoStatus)) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Não é possível mudar o alerta de " + alerta.getStatus() + " para " + novoStatus);
        }

        alerta.setStatus(novoStatus);
        alerta.setModeradorId(moderadorId);
        alerta.setUpdatedAt(new Date());
        return AlertaResponse.from(repository.save(alerta));
    }

    private static boolean transicaoPermitida(StatusAlerta atual, StatusAlerta novo) {
        if (atual == null) {
            return true;
        }
        return switch (atual) {
            case ABERTO -> novo == StatusAlerta.EM_ANALISE || novo == StatusAlerta.RESOLVIDO;
            case EM_ANALISE -> novo == StatusAlerta.RESOLVIDO || novo == StatusAlerta.ABERTO;
            case RESOLVIDO -> false;
        };
    }
}
