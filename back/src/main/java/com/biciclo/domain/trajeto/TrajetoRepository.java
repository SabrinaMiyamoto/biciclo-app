package com.biciclo.domain.trajeto;

import com.biciclo.common.enums.TipoMovimento;
import com.biciclo.common.enums.TipoReferencia;
import com.biciclo.common.exception.ResourceNotFoundException;
import com.biciclo.common.firestore.FirestoreUtils;
import com.biciclo.domain.extrato.ExtratoPontos;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.SetOptions;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class TrajetoRepository {

    private static final String COLLECTION = "trajetos";
    private static final String COLECAO_EXTRATO = "extrato_pontos";
    private static final String COLECAO_USUARIOS = "usuarios";
    private static final int LIMITE_LISTAGEM = 50;

    private final Firestore firestore;

    public TrajetoRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public Trajeto save(Trajeto trajeto) {
        DocumentReference ref;
        if (trajeto.getId() == null || trajeto.getId().isBlank()) {
            ref = firestore.collection(COLLECTION).document();
            trajeto.setId(ref.getId());
        } else {
            ref = firestore.collection(COLLECTION).document(trajeto.getId());
        }
        FirestoreUtils.await(ref.set(trajeto));
        return trajeto;
    }

    /**
     * Persiste um trajeto aprovado, credita os pontos no saldo do ciclista e
     * registra a entrada no extrato — tudo na mesma transação (RNF01).
     */
    public Trajeto registrarAprovado(Trajeto trajeto) {
        DocumentReference usuarioRef = firestore.collection(COLECAO_USUARIOS)
                .document(trajeto.getCiclistaId());

        if (!FirestoreUtils.await(usuarioRef.get()).exists()) {
            throw new ResourceNotFoundException("Usuário não encontrado");
        }

        return FirestoreUtils.await(firestore.runTransaction(transaction -> {
            DocumentReference trajetoRef = trajeto.getId() == null || trajeto.getId().isBlank()
                    ? firestore.collection(COLLECTION).document()
                    : firestore.collection(COLLECTION).document(trajeto.getId());
            trajeto.setId(trajetoRef.getId());

            ExtratoPontos extrato = new ExtratoPontos();
            extrato.setCiclistaId(trajeto.getCiclistaId());
            extrato.setTipoMovimento(TipoMovimento.ENTRADA);
            extrato.setQuantidade(trajeto.getPontosGanhos());
            extrato.setReferenciaTipo(TipoReferencia.TRAJETO);
            extrato.setReferenciaId(trajeto.getId());
            extrato.setCreatedAt(trajeto.getCreatedAt());

            DocumentReference extratoRef = firestore.collection(COLECAO_EXTRATO).document();
            extrato.setId(extratoRef.getId());

            transaction.set(trajetoRef, trajeto);
            transaction.set(extratoRef, extrato);
            transaction.set(usuarioRef,
                    Map.of("saldoPontos", FieldValue.increment(trajeto.getPontosGanhos())),
                    SetOptions.merge());

            return trajeto;
        }));
    }

    public List<Trajeto> findByCiclistaIdOrderByCreatedAtDesc(String ciclistaId) {
        QuerySnapshot snapshot = FirestoreUtils.await(
                firestore.collection(COLLECTION)
                        .whereEqualTo("ciclistaId", ciclistaId)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(LIMITE_LISTAGEM)
                        .get());
        return snapshot.getDocuments().stream()
                .map(doc -> doc.toObject(Trajeto.class))
                .toList();
    }
}
