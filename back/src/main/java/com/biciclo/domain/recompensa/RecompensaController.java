package com.biciclo.domain.recompensa;

import com.biciclo.domain.cupom.dto.CupomResponse;
import com.biciclo.domain.recompensa.dto.RecompensaRequest;
import com.biciclo.domain.recompensa.dto.RecompensaResponse;
import com.biciclo.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recompensas")
public class RecompensaController {

    private final RecompensaService service;

    public RecompensaController(RecompensaService service) {
        this.service = service;
    }

    /** Catálogo público (filtros de estoque/validade feitos no service). */
    @GetMapping
    public List<RecompensaResponse> listar() {
        return service.listar();
    }

    /** Recompensas do parceiro autenticado, incluindo inativas/sem estoque. */
    @GetMapping("/minhas")
    @PreAuthorize("hasAnyRole('PARTNER','ADMIN')")
    public List<RecompensaResponse> minhas(@AuthenticationPrincipal UserPrincipal principal) {
        return service.minhas(principal);
    }

    @GetMapping("/{id}")
    public RecompensaResponse buscar(@PathVariable String id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PARTNER','ADMIN')")
    public RecompensaResponse criar(@AuthenticationPrincipal UserPrincipal principal,
                                    @Valid @RequestBody RecompensaRequest request) {
        return service.criar(principal, request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PARTNER','ADMIN')")
    public RecompensaResponse atualizar(@PathVariable String id,
                                        @AuthenticationPrincipal UserPrincipal principal,
                                        @Valid @RequestBody RecompensaRequest request) {
        return service.atualizar(id, principal, request);
    }

    /** Soft delete: mantém o documento para preservar cupons já emitidos. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('PARTNER','ADMIN')")
    public void desativar(@PathVariable String id,
                          @AuthenticationPrincipal UserPrincipal principal) {
        service.desativar(id, principal);
    }

    @PostMapping("/{id}/resgatar")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CYCLIST')")
    public CupomResponse resgatar(@PathVariable String id,
                                  @AuthenticationPrincipal UserPrincipal principal) {
        return service.resgatar(id, principal.id());
    }
}
