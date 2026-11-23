package com.biciclo.domain.alerta;

import com.biciclo.domain.alerta.dto.AlertaRequest;
import com.biciclo.domain.alerta.dto.AlertaResponse;
import com.biciclo.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alertas-infraestrutura")
public class AlertaController {

    private final AlertaService service;

    public AlertaController(AlertaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CYCLIST')")
    public AlertaResponse registrar(@AuthenticationPrincipal UserPrincipal principal,
                                    @Valid @RequestBody AlertaRequest request) {
        return service.registrar(principal.id(), request);
    }

    @GetMapping("/minhas")
    @PreAuthorize("hasRole('CYCLIST')")
    public List<AlertaResponse> minhas(@AuthenticationPrincipal UserPrincipal principal) {
        return service.minhas(principal.id());
    }
}
