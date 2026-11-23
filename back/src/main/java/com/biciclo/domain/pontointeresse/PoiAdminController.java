package com.biciclo.domain.pontointeresse;

import com.biciclo.domain.pontointeresse.dto.PoiRequest;
import com.biciclo.domain.pontointeresse.dto.PoiResponse;
import com.biciclo.domain.pontointeresse.osm.ImportacaoOsmResponse;
import com.biciclo.domain.pontointeresse.osm.ImportacaoOsmService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cadastro de pontos de interesse e importação do OpenStreetMap (ADMIN).
 */
@RestController
@RequestMapping("/api/v1/admin/pois")
@PreAuthorize("hasRole('ADMIN')")
public class PoiAdminController {

    private final PontoInteresseService service;
    private final ImportacaoOsmService importacaoOsmService;

    public PoiAdminController(PontoInteresseService service, ImportacaoOsmService importacaoOsmService) {
        this.service = service;
        this.importacaoOsmService = importacaoOsmService;
    }

    @GetMapping("/{id}")
    public PoiResponse buscar(@PathVariable String id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PoiResponse criar(@Valid @RequestBody PoiRequest request) {
        return service.criar(request);
    }

    @PutMapping("/{id}")
    public PoiResponse atualizar(@PathVariable String id, @Valid @RequestBody PoiRequest request) {
        return service.atualizar(id, request);
    }

    /** Soft delete: o POI deixa de aparecer na busca pública. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(@PathVariable String id) {
        service.desativar(id);
    }

    /**
     * Importa bicicletários, oficinas, pontos de apoio e terminais do OSM.
     * Com forcar=true ignora o cache de 24 h e consulta a Overpass agora.
     */
    @PostMapping("/importar-osm")
    public ImportacaoOsmResponse importarOsm(@RequestParam(defaultValue = "false") boolean forcar) {
        return importacaoOsmService.importar(forcar);
    }
}
