package com.biciclo.domain.pontointeresse;

import ch.hsr.geohash.GeoHash;
import com.biciclo.common.enums.TipoPoi;
import com.biciclo.common.firestore.FirestoreUtils;
import com.biciclo.common.geo.GeoUtils;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class PontoInteresseRepository {

    private static final String COLLECTION = "pontos_interesse";

    /**
     * Precisão do geohash (5 caracteres ≈ células de ~4,9 km).
     * A busca cobre o centro + 8 vizinhos (grade 3x3), suficiente para o raio
     * máximo de 5 km, com filtro fino por distância (haversine) em memória.
     */
    private static final int GEOHASH_PRECISION = 5;

    /** Limite do Firestore é 500 operações por WriteBatch. */
    private static final int TAMANHO_LOTE = 400;

    private final Firestore firestore;

    public PontoInteresseRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public List<PontoInteresse> buscarProximos(double lat, double lng, double raio, int limite) {
        List<String> celulas = celulasVizinhas(lat, lng);

        QuerySnapshot snapshot = FirestoreUtils.await(
                firestore.collection(COLLECTION)
                        .whereEqualTo("ativo", true)
                        .whereIn("geohash", celulas)
                        .get());

        return snapshot.getDocuments().stream()
                .map(doc -> doc.toObject(PontoInteresse.class))
                .filter(poi -> poi.getLocalizacao() != null)
                .filter(poi -> GeoUtils.distanciaMetros(lat, lng,
                        poi.getLocalizacao().getLatitude(),
                        poi.getLocalizacao().getLongitude()) <= raio)
                .sorted(Comparator.comparingDouble(poi -> GeoUtils.distanciaMetros(lat, lng,
                        poi.getLocalizacao().getLatitude(),
                        poi.getLocalizacao().getLongitude())))
                .limit(limite)
                .toList();
    }

    public List<PontoInteresse> findByTipoAndAtivoTrue(TipoPoi tipo) {
        QuerySnapshot snapshot = FirestoreUtils.await(
                firestore.collection(COLLECTION)
                        .whereEqualTo("tipo", tipo)
                        .whereEqualTo("ativo", true)
                        .get());
        return snapshot.getDocuments().stream()
                .map(doc -> doc.toObject(PontoInteresse.class))
                .toList();
    }

    public Optional<PontoInteresse> findById(String id) {
        DocumentSnapshot doc = FirestoreUtils.await(
                firestore.collection(COLLECTION).document(id).get());
        if (!doc.exists()) {
            return Optional.empty();
        }
        return Optional.of(doc.toObject(PontoInteresse.class));
    }

    /** Busca vários documentos de uma vez (ids inexistentes são ignorados). */
    public Map<String, PontoInteresse> findAllById(Collection<String> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        DocumentReference[] refs = ids.stream()
                .map(id -> firestore.collection(COLLECTION).document(id))
                .toArray(DocumentReference[]::new);
        Map<String, PontoInteresse> encontrados = new HashMap<>();
        for (DocumentSnapshot doc : FirestoreUtils.await(firestore.getAll(refs))) {
            if (doc.exists()) {
                encontrados.put(doc.getId(), doc.toObject(PontoInteresse.class));
            }
        }
        return encontrados;
    }

    /**
     * Grava em lotes (WriteBatch, até 500 operações por lote). Os POIs
     * precisam ter id definido.
     */
    public void saveAll(List<PontoInteresse> pois) {
        for (int inicio = 0; inicio < pois.size(); inicio += TAMANHO_LOTE) {
            WriteBatch batch = firestore.batch();
            for (PontoInteresse poi : pois.subList(inicio, Math.min(inicio + TAMANHO_LOTE, pois.size()))) {
                atualizarGeohash(poi);
                batch.set(firestore.collection(COLLECTION).document(poi.getId()), poi);
            }
            FirestoreUtils.await(batch.commit());
        }
    }

    public PontoInteresse save(PontoInteresse poi) {
        DocumentReference ref;
        if (poi.getId() == null || poi.getId().isBlank()) {
            ref = firestore.collection(COLLECTION).document();
            poi.setId(ref.getId());
        } else {
            ref = firestore.collection(COLLECTION).document(poi.getId());
        }
        atualizarGeohash(poi);
        FirestoreUtils.await(ref.set(poi));
        return poi;
    }

    private void atualizarGeohash(PontoInteresse poi) {
        if (poi.getLocalizacao() != null) {
            poi.setGeohash(GeoHash.withCharacterPrecision(
                    poi.getLocalizacao().getLatitude(),
                    poi.getLocalizacao().getLongitude(),
                    GEOHASH_PRECISION).toBase32());
        }
    }

    private List<String> celulasVizinhas(double lat, double lng) {
        GeoHash centro = GeoHash.withCharacterPrecision(lat, lng, GEOHASH_PRECISION);
        List<String> celulas = new ArrayList<>();
        celulas.add(centro.toBase32());
        for (GeoHash vizinho : centro.getAdjacent()) {
            celulas.add(vizinho.toBase32());
        }
        return celulas;
    }
}
