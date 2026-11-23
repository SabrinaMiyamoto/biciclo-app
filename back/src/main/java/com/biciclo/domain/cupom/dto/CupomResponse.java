package com.biciclo.domain.cupom.dto;

import com.biciclo.common.enums.StatusCupom;
import com.biciclo.domain.cupom.Cupom;

import java.time.Instant;

public record CupomResponse(
        String id,
        String codigoValidacao,
        String recompensaId,
        String ciclistaId,
        StatusCupom status,
        Instant dataResgate,
        Instant dataUtilizacao
) {

    public static CupomResponse from(Cupom cupom) {
        return new CupomResponse(
                cupom.getId(),
                cupom.getCodigoValidacao(),
                cupom.getRecompensaId(),
                cupom.getCiclistaId(),
                cupom.getStatus(),
                cupom.getDataResgate() == null ? null : cupom.getDataResgate().toInstant(),
                cupom.getDataUtilizacao() == null ? null : cupom.getDataUtilizacao().toInstant());
    }
}
