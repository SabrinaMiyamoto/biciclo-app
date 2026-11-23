package com.biciclo.domain.alerta;

import com.biciclo.common.enums.StatusAlerta;
import com.google.cloud.firestore.GeoPoint;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class AlertaInfraestrutura {

    @DocumentId
    private String id;

    private String ciclistaId;

    private String tipo;

    private String descricao;

    private GeoPoint localizacao;

    private String fotoUrl;

    private StatusAlerta status = StatusAlerta.ABERTO;

    private Date createdAt;

    /** Última mudança de status feita por um ADMIN. */
    private Date updatedAt;

    /** Id do ADMIN que fez a última moderação. */
    private String moderadorId;
}
