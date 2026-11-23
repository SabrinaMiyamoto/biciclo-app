package com.biciclo.domain.pontointeresse.osm;

import com.biciclo.common.enums.TipoPoi;
import com.biciclo.domain.pontointeresse.PontoInteresse;
import com.google.cloud.firestore.GeoPoint;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

/**
 * Converte elementos do OpenStreetMap em pontos de interesse do Biciclo.
 */
public final class OsmPoiMapper {

    public static final String ORIGEM_OSM = "OSM";

    private static final int TAMANHO_MAXIMO_DESCRICAO = 200;

    private OsmPoiMapper() {
    }

    /** Id estável do documento, para reimportar sem duplicar: "osm-node-123". */
    public static String documentoId(OsmElemento elemento) {
        return "osm-" + elemento.tipo() + "-" + elemento.id();
    }

    public static Optional<TipoPoi> tipo(Map<String, String> tags) {
        String amenity = tags.getOrDefault("amenity", "");
        if ("bicycle".equals(tags.get("shop"))) {
            return Optional.of(TipoPoi.OFICINA);
        }
        return switch (amenity) {
            case "bicycle_parking" -> Optional.of(TipoPoi.BICICLETARIO);
            case "bicycle_repair_station", "bicycle_rental" -> Optional.of(TipoPoi.PONTO_APOIO);
            case "bus_station" -> Optional.of(TipoPoi.TERMINAL);
            default -> Optional.empty();
        };
    }

    public static Optional<PontoInteresse> paraPoi(OsmElemento elemento) {
        Map<String, String> tags = elemento.tags() == null ? Map.of() : elemento.tags();
        return tipo(tags).map(tipo -> {
            PontoInteresse poi = new PontoInteresse();
            poi.setId(documentoId(elemento));
            poi.setTipo(tipo);
            poi.setDescricao(descricao(tipo, tags));
            poi.setLocalizacao(new GeoPoint(elemento.latitude(), elemento.longitude()));
            poi.setCapacidade(capacidade(tags.get("capacity")));
            poi.setAtivo(true);
            poi.setOrigem(ORIGEM_OSM);
            poi.setOsmId(elemento.referencia());
            poi.setUpdatedAt(new Date());
            return poi;
        });
    }

    static String descricao(TipoPoi tipo, Map<String, String> tags) {
        String nome = tags.get("name");
        if (nome == null || nome.isBlank()) {
            nome = switch (tipo) {
                case BICICLETARIO -> "Bicicletário";
                case OFICINA -> "Oficina de bicicletas";
                case PONTO_APOIO -> "bicycle_rental".equals(tags.get("amenity"))
                        ? "Aluguel de bicicletas"
                        : "Estação de reparo";
                case TERMINAL -> "Terminal de ônibus";
            };
        }
        nome = nome.trim();
        return nome.length() > TAMANHO_MAXIMO_DESCRICAO ? nome.substring(0, TAMANHO_MAXIMO_DESCRICAO) : nome;
    }

    static Integer capacidade(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            int capacidade = Integer.parseInt(valor.trim());
            return capacidade >= 0 ? capacidade : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
