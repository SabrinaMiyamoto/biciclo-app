package com.biciclo.domain.usuario;

import com.biciclo.domain.extrato.dto.ExtratoResponse;
import com.biciclo.domain.usuario.dto.UsuarioResponse;
import com.biciclo.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return service.me(principal.id());
    }

    @GetMapping("/me/extrato")
    public List<ExtratoResponse> extrato(@AuthenticationPrincipal UserPrincipal principal) {
        return service.extrato(principal.id());
    }
}
