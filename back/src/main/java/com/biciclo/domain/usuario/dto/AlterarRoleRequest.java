package com.biciclo.domain.usuario.dto;

import com.biciclo.common.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlterarRoleRequest {

    @NotNull
    private Role role;
}
