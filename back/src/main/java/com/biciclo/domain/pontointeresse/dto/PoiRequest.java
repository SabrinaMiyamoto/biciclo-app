package com.biciclo.domain.pontointeresse.dto;

import com.biciclo.common.enums.TipoPoi;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PoiRequest {

    @NotNull
    private TipoPoi tipo;

    @NotBlank
    @Size(max = 200)
    private String descricao;

    @NotNull
    @DecimalMin("-90")
    @DecimalMax("90")
    private Double latitude;

    @NotNull
    @DecimalMin("-180")
    @DecimalMax("180")
    private Double longitude;

    @Min(0)
    private Integer capacidade;

    /** Opcional na edição: permite reativar um POI desativado. */
    private Boolean ativo;
}
