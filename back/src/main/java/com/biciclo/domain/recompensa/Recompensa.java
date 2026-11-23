package com.biciclo.domain.recompensa;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Recompensa {

    @DocumentId
    private String id;

    private String parceiroId;

    private String titulo;

    private String descricao;

    private Integer custoPontos;

    private Integer quantidadeEstoque = 0;

    private String dataExpiracao;

    private Boolean ativa = true;

    /**
     * Indica se a recompensa passou da validade. Delega o parsing para
     * {@link ValidadeRecompensa}; nulo/vazio = sem validade.
     */
    public boolean expirada() {
        return ValidadeRecompensa.expirada(dataExpiracao);
    }
}
