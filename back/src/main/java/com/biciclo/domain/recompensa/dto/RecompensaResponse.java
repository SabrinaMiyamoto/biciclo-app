package com.biciclo.domain.recompensa.dto;

import com.biciclo.domain.recompensa.Recompensa;

public record RecompensaResponse(
        String id,
        String parceiroId,
        String titulo,
        String descricao,
        Integer custoPontos,
        Integer quantidadeEstoque,
        String dataExpiracao,
        Boolean ativa
) {

    public static RecompensaResponse from(Recompensa recompensa) {
        return new RecompensaResponse(
                recompensa.getId(),
                recompensa.getParceiroId(),
                recompensa.getTitulo(),
                recompensa.getDescricao(),
                recompensa.getCustoPontos(),
                recompensa.getQuantidadeEstoque(),
                recompensa.getDataExpiracao(),
                recompensa.getAtiva());
    }
}
