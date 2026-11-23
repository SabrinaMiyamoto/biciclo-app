package com.biciclo.domain.alerta.dto;

import com.biciclo.common.enums.StatusAlerta;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AtualizarStatusAlertaRequest {

    @NotNull
    private StatusAlerta status;
}
