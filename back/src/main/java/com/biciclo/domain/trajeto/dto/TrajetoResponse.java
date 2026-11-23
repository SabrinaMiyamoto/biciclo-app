package com.biciclo.domain.trajeto.dto;

import com.biciclo.common.enums.StatusAnalise;
import com.biciclo.domain.trajeto.Trajeto;

import java.time.Instant;

public record TrajetoResponse(
        String id,
        Double distanciaMetros,
        Integer duracaoSegundos,
        Double velocidadeMedia,
        Integer pontosGanhos,
        StatusAnalise statusAnalise,
        Instant createdAt
) {

    public static TrajetoResponse from(Trajeto trajeto) {
        return new TrajetoResponse(
                trajeto.getId(),
                trajeto.getDistanciaMetros(),
                trajeto.getDuracaoSegundos(),
                trajeto.getVelocidadeMedia(),
                trajeto.getPontosGanhos(),
                trajeto.getStatusAnalise(),
                trajeto.getCreatedAt() == null ? null : trajeto.getCreatedAt().toInstant());
    }
}
