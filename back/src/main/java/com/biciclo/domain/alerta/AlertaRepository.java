package com.biciclo.domain.alerta;

import com.biciclo.common.enums.StatusAlerta;
import com.biciclo.common.firestore.FirestoreUtils;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AlertaRepository {

    private static final String COLLECTION = "alertas_infraestrutura";

    private final Firestore firestore;

    public AlertaRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public AlertaInfraestrutura save(AlertaInfraestrutura alerta) {
        DocumentReference ref;
        if (alerta.getId() == null || alerta.getId().isBlank()) {
            ref = firestore.collection(COLLECTION).document();
            alerta.setId(ref.getId());
        } else {
            ref = firestore.collection(COLLECTION).document(alerta.getId());
        }
        FirestoreUtils.await(ref.set(alerta));
        return alerta;
    }

    public Optional<AlertaInfraestrutura> findById(String id) {
        DocumentSnapshot doc = FirestoreUtils.await(
                firestore.collection(COLLECTION).document(id).get());
        if (!doc.exists()) {
            return Optional.empty();
        }
        return Optional.of(doc.toObject(AlertaInfraestrutura.class));
    }

    /** Alertas mais recentes primeiro; filtra por status quando informado. */
    public List<AlertaInfraestrutura> findByStatus(StatusAlerta status, int limite) {
        Query query = firestore.collection(COLLECTION);
        if (status != null) {
            query = query.whereEqualTo("status", status);
        }
        return listar(query, limite);
    }

    public List<AlertaInfraestrutura> findByCiclistaId(String ciclistaId, int limite) {
        return listar(firestore.collection(COLLECTION).whereEqualTo("ciclistaId", ciclistaId), limite);
    }

    private List<AlertaInfraestrutura> listar(Query query, int limite) {
        QuerySnapshot snapshot = FirestoreUtils.await(
                query.orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(limite)
                        .get());
        return snapshot.getDocuments().stream()
                .map(doc -> doc.toObject(AlertaInfraestrutura.class))
                .toList();
    }
}
