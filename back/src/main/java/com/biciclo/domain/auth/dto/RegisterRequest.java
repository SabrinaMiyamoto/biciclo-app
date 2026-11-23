package com.biciclo.domain.auth.dto;

import com.biciclo.common.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Cadastro de perfil. A conta em si é criada no Firebase Auth pelo cliente
 * (Google ou e-mail/senha); a API recebe apenas o ID token resultante.
 */
@Getter
@Setter
public class RegisterRequest {

    /** ID token emitido pelo Firebase Auth logo após o cadastro no cliente. */
    @NotBlank
    private String idToken;

    @NotNull
    private Role role;

    @Size(max = 150)
    private String nome;

    // Campos obrigatórios apenas quando role = PARTNER
    private String nomeFantasia;

    private String cnpj;

    private Double latitude;

    private Double longitude;
}
