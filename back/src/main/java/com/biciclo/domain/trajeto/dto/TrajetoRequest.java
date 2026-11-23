package com.biciclo.domain.trajeto.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TrajetoRequest {

    @Valid
    @NotNull
    @Size(min = 2, max = 5000, message = "O trajeto deve ter entre 2 e 5000 pontos")
    private List<PontoTrajetoRequest> pontos;
}
