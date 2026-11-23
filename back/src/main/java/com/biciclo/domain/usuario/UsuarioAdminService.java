package com.biciclo.domain.usuario;

import com.biciclo.common.enums.Role;
import com.biciclo.common.exception.BusinessException;
import com.biciclo.common.exception.ResourceNotFoundException;
import com.biciclo.domain.usuario.dto.UsuarioResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Gestão de usuários pela área administrativa.
 */
@Service
public class UsuarioAdminService {

    private static final int LIMITE_LISTAGEM = 100;

    private final UsuarioRepository repository;

    public UsuarioAdminService(UsuarioRepository repository) {
        this.repository = repository;
    }

    public List<UsuarioResponse> listar(Role role) {
        return repository.findAll(role, LIMITE_LISTAGEM).stream()
                .map(UsuarioResponse::from)
                .toList();
    }

    /**
     * Promove um ciclista a ADMIN ou rebaixa um ADMIN a ciclista.
     * Parceiros dependem do cadastro com CNPJ, então não entram nesta troca.
     * O novo perfil vale a partir do próximo login (o JWT atual guarda o antigo).
     */
    public UsuarioResponse alterarRole(String usuarioId, Role novaRole, String adminId) {
        if (novaRole != Role.ADMIN && novaRole != Role.CYCLIST) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Só é possível alternar entre CYCLIST e ADMIN");
        }
        if (usuarioId.equals(adminId)) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Um administrador não pode alterar o próprio perfil");
        }

        Usuario usuario = repository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (usuario.getRole() == Role.PARTNER) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Perfil de parceiro não pode ser alterado por esta rota");
        }
        if (usuario.getRole() != novaRole) {
            usuario.setRole(novaRole);
            repository.save(usuario);
        }
        return UsuarioResponse.from(usuario);
    }
}
