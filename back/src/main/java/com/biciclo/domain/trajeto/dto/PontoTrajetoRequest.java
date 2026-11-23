package com.biciclo.domain.trajeto.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record PontoTrajetoRequest(
        @NotNull Double latitude,
        @NotNull Double longitude,
        @NotNull Instant timestamp
) {
}
