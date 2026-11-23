package com.biciclo.domain.pontointeresse;

import com.biciclo.common.enums.TipoPoi;
import com.google.cloud.firestore.GeoPoint;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class PontoInteresse {

    @DocumentId
    private String id;

    private TipoPoi tipo;

    private String descricao;

    private GeoPoint localizacao;

    private String geohash;

    private Integer capacidade;

    private Boolean ativo = true;

    /** "MANUAL" (cadastrado por ADMIN) ou "OSM" (importado do OpenStreetMap). */
    private String origem;

    /** Referência no OSM, ex.: "node/123". Só para origem OSM. */
    private String osmId;

    private Date updatedAt;
}
