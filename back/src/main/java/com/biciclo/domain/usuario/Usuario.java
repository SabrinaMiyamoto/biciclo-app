package com.biciclo.domain.usuario;

import com.biciclo.common.enums.Role;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @DocumentId
    private String id;

    /**
     * UID do usuário no Firebase Auth (fonte única de identidade).
     * Para contas novas o próprio id do documento também é o UID.
     */
    private String firebaseUid;

    private String nome;

    private String email;

    /**
     * Legado: senha local (BCrypt). Não é mais utilizada — a autenticação
     * acontece exclusivamente no Firebase Auth. Mantido apenas para
     * compatibilidade com documentos antigos do Firestore.
     */
    private String senhaHash;

    private Role role;

    /** Provedor usado no login: "google.com", "password", etc. */
    private String provedor;

    private String fotoUrl;

    private Integer saldoPontos = 0;

    private Date createdAt;
}
