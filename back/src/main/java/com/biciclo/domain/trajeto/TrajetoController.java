package com.biciclo.domain.trajeto;

import com.biciclo.domain.trajeto.dto.TrajetoRequest;
import com.biciclo.domain.trajeto.dto.TrajetoResponse;
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
@RequestMapping("/api/v1/trajetos")
public class TrajetoController {

    private final TrajetoService service;

    public TrajetoController(TrajetoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CYCLIST')")
    public TrajetoResponse registrar(@AuthenticationPrincipal UserPrincipal principal,
                                     @Valid @RequestBody TrajetoRequest request) {
        return service.registrar(principal.id(), request);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CYCLIST')")
    public List<TrajetoResponse> listar(@AuthenticationPrincipal UserPrincipal principal) {
        return service.listarDoCiclista(principal.id());
    }
}
