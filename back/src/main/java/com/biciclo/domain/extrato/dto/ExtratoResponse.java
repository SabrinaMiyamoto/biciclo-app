package com.biciclo.domain.extrato.dto;

import com.biciclo.common.enums.TipoMovimento;
import com.biciclo.common.enums.TipoReferencia;
import com.biciclo.domain.extrato.ExtratoPontos;

import java.time.Instant;

public record ExtratoResponse(
        String id,
        TipoMovimento tipoMovimento,
        Integer quantidade,
        TipoReferencia referenciaTipo,
        String referenciaId,
        Instant createdAt
) {

    public static ExtratoResponse from(ExtratoPontos extrato) {
        return new ExtratoResponse(
                extrato.getId(),
                extrato.getTipoMovimento(),
                extrato.getQuantidade(),
                extrato.getReferenciaTipo(),
                extrato.getReferenciaId(),
                extrato.getCreatedAt() == null ? null : extrato.getCreatedAt().toInstant());
    }
}
