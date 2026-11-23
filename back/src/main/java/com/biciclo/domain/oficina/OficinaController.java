package com.biciclo.domain.oficina;

import com.biciclo.domain.oficina.dto.OficinaResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exige login (qualquer perfil): cada chamada consome cota paga do Google.
 */
@RestController
@RequestMapping("/api/v1/oficinas")
public class OficinaController {

    private final OficinaService service;

    public OficinaController(OficinaService service) {
        this.service = service;
    }

    @GetMapping
    public List<OficinaResponse> listar(@RequestParam double latitude,
                                        @RequestParam double longitude,
                                        @RequestParam(defaultValue = "2000") double raio) {
        return service.buscarProximas(latitude, longitude, raio);
    }
}
