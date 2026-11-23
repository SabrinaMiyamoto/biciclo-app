package com.biciclo.domain.extrato;

import com.biciclo.common.firestore.FirestoreUtils;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ExtratoPontosRepository {

    private static final String COLLECTION = "extrato_pontos";

    private final Firestore firestore;

    public ExtratoPontosRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public List<ExtratoPontos> findByCiclistaIdOrderByCreatedAtDesc(String ciclistaId) {
        QuerySnapshot snapshot = FirestoreUtils.await(
                firestore.collection(COLLECTION)
                        .whereEqualTo("ciclistaId", ciclistaId)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .get());
        return snapshot.getDocuments().stream()
                .map(doc -> doc.toObject(ExtratoPontos.class))
                .toList();
    }

    public ExtratoPontos save(ExtratoPontos extrato) {
        DocumentReference ref;
        if (extrato.getId() == null || extrato.getId().isBlank()) {
            ref = firestore.collection(COLLECTION).document();
            extrato.setId(ref.getId());
        } else {
            ref = firestore.collection(COLLECTION).document(extrato.getId());
        }
        FirestoreUtils.await(ref.set(extrato));
        return extrato;
    }
}
