package com.biciclo.domain.extrato;

import com.biciclo.common.enums.TipoMovimento;
import com.biciclo.common.enums.TipoReferencia;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class ExtratoPontos {

    @DocumentId
    private String id;

    private String ciclistaId;

    private TipoMovimento tipoMovimento;

    private Integer quantidade;

    private TipoReferencia referenciaTipo;

    private String referenciaId;

    private Date createdAt;
}
