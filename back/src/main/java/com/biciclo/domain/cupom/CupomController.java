package com.biciclo.domain.cupom;

import com.biciclo.domain.cupom.dto.CupomResponse;
import com.biciclo.domain.cupom.dto.ValidarCupomRequest;
import com.biciclo.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cupons")
public class CupomController {

    private final CupomService service;

    public CupomController(CupomService service) {
        this.service = service;
    }

    @GetMapping("/meus")
    @PreAuthorize("hasRole('CYCLIST')")
    public List<CupomResponse> meus(@AuthenticationPrincipal UserPrincipal principal) {
        return service.meus(principal.id());
    }

    @PostMapping("/validar")
    @PreAuthorize("hasRole('PARTNER')")
    public CupomResponse validar(@Valid @RequestBody ValidarCupomRequest request,
                                 @AuthenticationPrincipal UserPrincipal principal) {
        return service.validar(request.codigo(), principal.id());
    }
}
