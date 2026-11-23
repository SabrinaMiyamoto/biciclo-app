package com.biciclo.domain.pontointeresse.osm;

import com.biciclo.domain.pontointeresse.PontoInteresse;
import com.biciclo.domain.pontointeresse.PontoInteresseRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Importa a infraestrutura cicloviária do OpenStreetMap para o Firestore.
 * Reimportar é seguro: o id do documento vem do id do OSM, e POIs
 * desativados por um ADMIN continuam desativados.
 */
@Service
public class ImportacaoOsmService {

    private final OverpassClient overpassClient;
    private final PontoInteresseRepository repository;

    public ImportacaoOsmService(OverpassClient overpassClient, PontoInteresseRepository repository) {
        this.overpassClient = overpassClient;
        this.repository = repository;
    }

    /**
     * @param forcarAtualizacao true ignora o cache e consulta a Overpass agora
     */
    public ImportacaoOsmResponse importar(boolean forcarAtualizacao) {
        List<OsmElemento> elementos = forcarAtualizacao
                ? overpassClient.atualizarInfraestrutura()
                : overpassClient.buscarInfraestrutura();

        Map<String, PontoInteresse> candidatos = new LinkedHashMap<>();
        int ignorados = 0;
        for (OsmElemento elemento : elementos) {
            Optional<PontoInteresse> poi = OsmPoiMapper.paraPoi(elemento);
            if (poi.isPresent()) {
                candidatos.put(poi.get().getId(), poi.get());
            } else {
                ignorados++;
            }
        }

        Map<String, PontoInteresse> existentes = repository.findAllById(candidatos.keySet());

        List<PontoInteresse> paraGravar = new ArrayList<>();
        int criados = 0;
        int atualizados = 0;
        for (PontoInteresse poi : candidatos.values()) {
            PontoInteresse atual = existentes.get(poi.getId());
            if (atual == null) {
                criados++;
            } else if (Boolean.FALSE.equals(atual.getAtivo())) {
                ignorados++;
                continue;
            } else {
                atualizados++;
            }
            paraGravar.add(poi);
        }

        repository.saveAll(paraGravar);
        return new ImportacaoOsmResponse(elementos.size(), criados, atualizados, ignorados);
    }
}
