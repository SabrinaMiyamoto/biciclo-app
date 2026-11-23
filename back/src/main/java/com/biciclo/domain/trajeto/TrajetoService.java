package com.biciclo.domain.trajeto;

import com.biciclo.common.enums.StatusAnalise;
import com.biciclo.common.exception.BusinessException;
import com.biciclo.common.geo.GeoUtils;
import com.biciclo.domain.trajeto.dto.PontoTrajetoRequest;
import com.biciclo.domain.trajeto.dto.TrajetoRequest;
import com.biciclo.domain.trajeto.dto.TrajetoResponse;
import com.google.cloud.firestore.GeoPoint;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Regras de gamificação dos trajetos (RF08–RF11).
 *
 * A distância, a duração e a velocidade são calculadas no servidor a partir
 * dos pontos enviados pelo cliente, para não confiar em métricas prontas.
 * Trajetos muito curtos ou com velocidade incompatível com bicicleta são
 * rejeitados e não geram pontos (anti-fraude).
 */
@Service
public class TrajetoService {

    /** 10 pontos por quilômetro percorrido. */
    private static final double PONTOS_POR_KM = 10.0;
    private static final double DISTANCIA_MINIMA_METROS = 200.0;
    private static final double VELOCIDADE_MAXIMA_KMH = 60.0;
    private static final long TOLERANCIA_FUTURO_SEGUNDOS = 300;

    private final TrajetoRepository repository;

    public TrajetoService(TrajetoRepository repository) {
        this.repository = repository;
    }

    public TrajetoResponse registrar(String ciclistaId, TrajetoRequest request) {
        List<PontoTrajetoRequest> pontos = request.getPontos();
        validarCoordenadas(pontos);

        double distanciaMetros = calcularDistancia(pontos);
        int duracaoSegundos = calcularDuracao(pontos);
        double velocidadeKmh = (distanciaMetros / 1000.0) / (duracaoSegundos / 3600.0);

        StatusAnalise status = avaliar(distanciaMetros, velocidadeKmh);
        int pontosGanhos = status == StatusAnalise.APROVADO
                ? calcularPontos(distanciaMetros)
                : 0;

        Trajeto trajeto = new Trajeto();
        trajeto.setCiclistaId(ciclistaId);
        trajeto.setTrajeto(pontos.stream()
                .map(ponto -> new GeoPoint(ponto.latitude(), ponto.longitude()))
                .toList());
        trajeto.setDistanciaMetros(arredondar(distanciaMetros, 1));
        trajeto.setDuracaoSegundos(duracaoSegundos);
        trajeto.setVelocidadeMedia(arredondar(velocidadeKmh, 2));
        trajeto.setPontosGanhos(pontosGanhos);
        trajeto.setStatusAnalise(status);
        trajeto.setCreatedAt(new Date());

        if (status == StatusAnalise.APROVADO && pontosGanhos > 0) {
            repository.registrarAprovado(trajeto);
        } else {
            repository.save(trajeto);
        }

        return TrajetoResponse.from(trajeto);
    }

    public List<TrajetoResponse> listarDoCiclista(String ciclistaId) {
        return repository.findByCiclistaIdOrderByCreatedAtDesc(ciclistaId).stream()
                .map(TrajetoResponse::from)
                .toList();
    }

    private void validarCoordenadas(List<PontoTrajetoRequest> pontos) {
        for (PontoTrajetoRequest ponto : pontos) {
            if (ponto.latitude() < -90 || ponto.latitude() > 90) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Latitude inválida");
            }
            if (ponto.longitude() < -180 || ponto.longitude() > 180) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Longitude inválida");
            }
        }
    }

    private double calcularDistancia(List<PontoTrajetoRequest> pontos) {
        double total = 0;
        for (int i = 1; i < pontos.size(); i++) {
            PontoTrajetoRequest anterior = pontos.get(i - 1);
            PontoTrajetoRequest atual = pontos.get(i);
            total += GeoUtils.distanciaMetros(
                    anterior.latitude(), anterior.longitude(),
                    atual.latitude(), atual.longitude());
        }
        return total;
    }

    private int calcularDuracao(List<PontoTrajetoRequest> pontos) {
        Instant inicio = pontos.get(0).timestamp();
        Instant fim = pontos.get(pontos.size() - 1).timestamp();

        if (fim.isAfter(Instant.now().plusSeconds(TOLERANCIA_FUTURO_SEGUNDOS))) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Trajeto com data no futuro");
        }

        for (int i = 1; i < pontos.size(); i++) {
            if (pontos.get(i).timestamp().isBefore(pontos.get(i - 1).timestamp())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "Timestamps devem estar em ordem cronológica");
            }
        }

        long segundos = Duration.between(inicio, fim).getSeconds();
        if (segundos <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "A duração do trajeto deve ser maior que zero");
        }
        return (int) segundos;
    }

    private StatusAnalise avaliar(double distanciaMetros, double velocidadeKmh) {
        if (distanciaMetros < DISTANCIA_MINIMA_METROS) {
            return StatusAnalise.REJEITADO;
        }
        if (velocidadeKmh > VELOCIDADE_MAXIMA_KMH) {
            return StatusAnalise.REJEITADO;
        }
        return StatusAnalise.APROVADO;
    }

    private int calcularPontos(double distanciaMetros) {
        return (int) Math.floor((distanciaMetros / 1000.0) * PONTOS_POR_KM);
    }

    private double arredondar(double valor, int casas) {
        double fator = Math.pow(10, casas);
        return Math.round(valor * fator) / fator;
    }
}
