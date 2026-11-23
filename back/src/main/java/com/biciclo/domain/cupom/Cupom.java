package com.biciclo.domain.cupom;

import com.biciclo.common.enums.StatusCupom;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class Cupom {

    @DocumentId
    private String id;

    private String codigoValidacao;

    private String recompensaId;

    private String ciclistaId;

    private StatusCupom status = StatusCupom.PENDENTE;

    private Date dataResgate;

    private Date dataUtilizacao;
}
