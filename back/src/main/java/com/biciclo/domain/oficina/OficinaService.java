package com.biciclo.domain.oficina;

import com.biciclo.common.exception.BusinessException;
import com.biciclo.domain.oficina.dto.OficinaResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Oficinas de bicicleta com nota e horário, buscadas ao vivo no Google Places.
 * Complementa os POIs do OpenStreetMap (bicicletários, estações de reparo).
 */
@Service
public class OficinaService {

    private static final double RAIO_MAXIMO_METROS = 5000;

    private final GooglePlacesClient client;

    public OficinaService(GooglePlacesClient client) {
        this.client = client;
    }

    public List<OficinaResponse> buscarProximas(double latitude, double longitude, double raio) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Coordenadas inválidas");
        }
        if (raio <= 0 || raio > RAIO_MAXIMO_METROS) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Raio deve ser maior que 0 e no máximo " + (int) RAIO_MAXIMO_METROS + " metros");
        }
        return client.buscarOficinas(latitude, longitude, raio);
    }
}
