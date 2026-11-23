package com.biciclo.domain.usuario;

import com.biciclo.common.enums.Role;
import com.biciclo.domain.usuario.dto.AlterarRoleRequest;
import com.biciclo.domain.usuario.dto.UsuarioResponse;
import com.biciclo.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioAdminController {

    private final UsuarioAdminService service;

    public UsuarioAdminController(UsuarioAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<UsuarioResponse> listar(@RequestParam(required = false) Role role) {
        return service.listar(role);
    }

    @PatchMapping("/{id}/role")
    public UsuarioResponse alterarRole(@PathVariable String id,
                                       @AuthenticationPrincipal UserPrincipal principal,
                                       @Valid @RequestBody AlterarRoleRequest request) {
        return service.alterarRole(id, request.getRole(), principal.id());
    }
}
