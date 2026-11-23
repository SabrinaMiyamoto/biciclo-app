package com.biciclo.domain.pontointeresse.dto;

import com.biciclo.common.enums.TipoPoi;
import com.biciclo.domain.pontointeresse.PontoInteresse;

public record PoiResponse(
        String id,
        TipoPoi tipo,
        String descricao,
        Double latitude,
        Double longitude,
        Integer capacidade,
        Boolean ativo,
        String origem
) {

    public static PoiResponse from(PontoInteresse poi) {
        return new PoiResponse(
                poi.getId(),
                poi.getTipo(),
                poi.getDescricao(),
                poi.getLocalizacao().getLatitude(),
                poi.getLocalizacao().getLongitude(),
                poi.getCapacidade(),
                poi.getAtivo(),
                poi.getOrigem()
        );
    }
}
