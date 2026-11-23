package com.biciclo.domain.parceiro;

import com.biciclo.common.firestore.FirestoreUtils;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.stereotype.Repository;

@Repository
public class ParceiroRepository {

    private static final String COLLECTION = "parceiros";

    private final Firestore firestore;

    public ParceiroRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public boolean existsByCnpj(String cnpj) {
        QuerySnapshot snapshot = FirestoreUtils.await(
                firestore.collection(COLLECTION)
                        .whereEqualTo("cnpj", cnpj)
                        .limit(1)
                        .get());
        return !snapshot.isEmpty();
    }

    public Parceiro save(Parceiro parceiro) {
        DocumentReference ref;
        if (parceiro.getId() == null || parceiro.getId().isBlank()) {
            ref = firestore.collection(COLLECTION).document();
            parceiro.setId(ref.getId());
        } else {
            ref = firestore.collection(COLLECTION).document(parceiro.getId());
        }
        FirestoreUtils.await(ref.set(parceiro));
        return parceiro;
    }
}
