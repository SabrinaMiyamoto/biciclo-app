package com.biciclo.domain.cupom.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidarCupomRequest(
        @NotBlank String codigo
) {
}
