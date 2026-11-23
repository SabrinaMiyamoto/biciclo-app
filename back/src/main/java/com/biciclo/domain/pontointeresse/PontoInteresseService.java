package com.biciclo.domain.pontointeresse;

import com.biciclo.common.exception.BusinessException;
import com.biciclo.common.exception.ResourceNotFoundException;
import com.biciclo.domain.pontointeresse.dto.PoiRequest;
import com.biciclo.domain.pontointeresse.dto.PoiResponse;
import com.google.cloud.firestore.GeoPoint;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class PontoInteresseService {

    static final String ORIGEM_MANUAL = "MANUAL";

    private static final int LIMITE_RESULTADOS = 50;
    private static final double RAIO_MAXIMO_METROS = 5000;

    private final PontoInteresseRepository repository;

    public PontoInteresseService(PontoInteresseRepository repository) {
        this.repository = repository;
    }

    /**
     * Busca POIs próximos de uma coordenada, dentro de um raio em metros.
     * Limita o raio a 5 km (RNF02).
     */
    public List<PoiResponse> buscarProximos(double latitude, double longitude, double raio) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Coordenadas inválidas");
        }
        if (raio <= 0 || raio > RAIO_MAXIMO_METROS) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Raio deve ser maior que 0 e no máximo " + (int) RAIO_MAXIMO_METROS + " metros");
        }

        return repository.buscarProximos(latitude, longitude, raio, LIMITE_RESULTADOS)
                .stream()
                .map(PoiResponse::from)
                .toList();
    }

    public PoiResponse buscar(String id) {
        return PoiResponse.from(encontrar(id));
    }

    /** Cadastro manual pela área administrativa. */
    public PoiResponse criar(PoiRequest request) {
        PontoInteresse poi = new PontoInteresse();
        poi.setOrigem(ORIGEM_MANUAL);
        aplicar(poi, request);
        poi.setAtivo(request.getAtivo() == null || request.getAtivo());
        return PoiResponse.from(repository.save(poi));
    }

    public PoiResponse atualizar(String id, PoiRequest request) {
        PontoInteresse poi = encontrar(id);
        aplicar(poi, request);
        if (request.getAtivo() != null) {
            poi.setAtivo(request.getAtivo());
        }
        return PoiResponse.from(repository.save(poi));
    }

    /** Soft delete: o POI some da busca, mas a reimportação do OSM não o recria. */
    public void desativar(String id) {
        PontoInteresse poi = encontrar(id);
        poi.setAtivo(false);
        poi.setUpdatedAt(new Date());
        repository.save(poi);
    }

    private PontoInteresse encontrar(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ponto de interesse não encontrado"));
    }

    private static void aplicar(PontoInteresse poi, PoiRequest request) {
        poi.setTipo(request.getTipo());
        poi.setDescricao(request.getDescricao().trim());
        poi.setLocalizacao(new GeoPoint(request.getLatitude(), request.getLongitude()));
        poi.setCapacidade(request.getCapacidade());
        poi.setUpdatedAt(new Date());
    }
}
