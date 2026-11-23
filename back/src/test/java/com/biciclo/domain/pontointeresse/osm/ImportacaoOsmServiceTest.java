package com.biciclo.domain.pontointeresse.osm;

import com.biciclo.domain.pontointeresse.PontoInteresse;
import com.biciclo.domain.pontointeresse.PontoInteresseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportacaoOsmServiceTest {

    @Mock
    private OverpassClient overpassClient;

    @Mock
    private PontoInteresseRepository repository;

    @InjectMocks
    private ImportacaoOsmService service;

    @Test
    @SuppressWarnings("unchecked")
    void criaAtualizaEIgnoraConformeEstado() {
        when(overpassClient.buscarInfraestrutura()).thenReturn(List.of(
                elemento(1, "bicycle_parking"),   // novo
                elemento(2, "bicycle_parking"),   // já existe ativo
                elemento(3, "bicycle_parking"),   // desativado por ADMIN
                elemento(4, "bench")));           // tipo desconhecido

        PontoInteresse ativo = new PontoInteresse();
        ativo.setAtivo(true);
        PontoInteresse desativado = new PontoInteresse();
        desativado.setAtivo(false);
        when(repository.findAllById(anyCollection())).thenReturn(Map.of(
                "osm-node-2", ativo,
                "osm-node-3", desativado));

        ImportacaoOsmResponse resposta = service.importar(false);

        assertEquals(new ImportacaoOsmResponse(4, 1, 1, 2), resposta);
        ArgumentCaptor<List<PontoInteresse>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertEquals(List.of("osm-node-1", "osm-node-2"),
                captor.getValue().stream().map(PontoInteresse::getId).toList());
        verify(overpassClient, never()).atualizarInfraestrutura();
    }

    @Test
    void forcarIgnoraCache() {
        when(overpassClient.atualizarInfraestrutura()).thenReturn(List.of());
        when(repository.findAllById(anyCollection())).thenReturn(Map.of());

        assertEquals(new ImportacaoOsmResponse(0, 0, 0, 0), service.importar(true));
        verify(overpassClient, never()).buscarInfraestrutura();
    }

    private static OsmElemento elemento(long id, String amenity) {
        return new OsmElemento("node", id, -27.59, -48.55, Map.of("amenity", amenity));
    }
}
