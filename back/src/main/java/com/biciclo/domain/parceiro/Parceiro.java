package com.biciclo.domain.parceiro;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Parceiro {

    @DocumentId
    private String id;

    private String usuarioId;

    private String nomeFantasia;

    private String cnpj;

    private Double latitude;

    private Double longitude;

    private Boolean ativo = true;
}
