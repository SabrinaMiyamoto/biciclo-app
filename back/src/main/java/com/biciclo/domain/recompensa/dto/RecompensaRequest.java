package com.biciclo.domain.recompensa.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecompensaRequest {

    @NotBlank
    @Size(max = 150)
    private String titulo;

    @Size(max = 500)
    private String descricao;

    @NotNull
    @Min(0)
    @Max(1_000_000)
    private Integer custoPontos;

    @NotNull
    @Min(0)
    @Max(1_000_000)
    private Integer quantidadeEstoque;

    @Size(max = 40)
    private String dataExpiracao;

    private Boolean ativa;

    /** Somente ADMIN pode informar/gerenciar outro parceiro. */
    @Size(max = 100)
    private String parceiroId;
}
