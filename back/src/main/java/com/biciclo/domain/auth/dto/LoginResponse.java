package com.biciclo.domain.auth.dto;

import com.biciclo.common.enums.Role;

public record LoginResponse(
        String token,
        String id,
        String nome,
        String email,
        Role role
) {
}
