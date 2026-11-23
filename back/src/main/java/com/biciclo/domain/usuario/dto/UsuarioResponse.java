package com.biciclo.domain.usuario.dto;

import com.biciclo.common.enums.Role;
import com.biciclo.domain.usuario.Usuario;

import java.time.Instant;

public record UsuarioResponse(
        String id,
        String nome,
        String email,
        Role role,
        String provedor,
        String fotoUrl,
        Integer saldoPontos,
        Instant createdAt
) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getRole(),
                usuario.getProvedor(),
                usuario.getFotoUrl(),
                usuario.getSaldoPontos(),
                usuario.getCreatedAt() == null ? null : usuario.getCreatedAt().toInstant());
    }
}
