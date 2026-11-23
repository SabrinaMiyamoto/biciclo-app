package com.biciclo.domain.alerta.dto;

import com.biciclo.common.enums.StatusAlerta;
import com.biciclo.domain.alerta.AlertaInfraestrutura;

import java.time.Instant;

public record AlertaResponse(
        String id,
        String ciclistaId,
        String tipo,
        String descricao,
        Double latitude,
        Double longitude,
        String fotoUrl,
        StatusAlerta status,
        Instant createdAt,
        Instant updatedAt
) {

    public static AlertaResponse from(AlertaInfraestrutura alerta) {
        return new AlertaResponse(
                alerta.getId(),
                alerta.getCiclistaId(),
                alerta.getTipo(),
                alerta.getDescricao(),
                alerta.getLocalizacao() == null ? null : alerta.getLocalizacao().getLatitude(),
                alerta.getLocalizacao() == null ? null : alerta.getLocalizacao().getLongitude(),
                alerta.getFotoUrl(),
                alerta.getStatus(),
                alerta.getCreatedAt() == null ? null : alerta.getCreatedAt().toInstant(),
                alerta.getUpdatedAt() == null ? null : alerta.getUpdatedAt().toInstant());
    }
}
