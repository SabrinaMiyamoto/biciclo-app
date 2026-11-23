package com.biciclo.domain.trajeto;

import com.biciclo.common.enums.StatusAnalise;
import com.google.cloud.firestore.GeoPoint;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Trajeto {

    @DocumentId
    private String id;

    private String ciclistaId;

    private List<GeoPoint> trajeto;

    private Double distanciaMetros;

    private Integer duracaoSegundos;

    private Double velocidadeMedia;

    private Integer pontosGanhos;

    private StatusAnalise statusAnalise = StatusAnalise.PENDENTE;

    private Date createdAt;
}
