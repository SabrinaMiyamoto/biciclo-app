package com.biciclo.domain.alerta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlertaRequest {

    @NotBlank
    @Size(max = 50)
    private String tipo;

    @NotBlank
    @Size(max = 500)
    private String descricao;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    @Size(max = 500)
    private String fotoUrl;
}
