package com.biciclo.domain.alerta;

import com.biciclo.common.enums.StatusAlerta;
import com.biciclo.domain.alerta.dto.AlertaResponse;
import com.biciclo.domain.alerta.dto.AtualizarStatusAlertaRequest;
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

/**
 * Moderação de alertas de infraestrutura (RF07) pela área administrativa.
 */
@RestController
@RequestMapping("/api/v1/admin/alertas")
@PreAuthorize("hasRole('ADMIN')")
public class AlertaAdminController {

    private final AlertaService service;

    public AlertaAdminController(AlertaService service) {
        this.service = service;
    }

    @GetMapping
    public List<AlertaResponse> listar(@RequestParam(required = false) StatusAlerta status) {
        return service.listar(status);
    }

    @PatchMapping("/{id}/status")
    public AlertaResponse atualizarStatus(@PathVariable String id,
                                          @AuthenticationPrincipal UserPrincipal principal,
                                          @Valid @RequestBody AtualizarStatusAlertaRequest request) {
        return service.atualizarStatus(id, request.getStatus(), principal.id());
    }
}
