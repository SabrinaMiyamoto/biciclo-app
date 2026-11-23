package com.biciclo.domain.cupom;

import com.biciclo.common.enums.StatusCupom;
import com.biciclo.common.firestore.FirestoreUtils;
import com.biciclo.domain.cupom.ValidacaoResultado.Status;
import com.biciclo.domain.recompensa.Recompensa;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public class CupomRepository {

    private static final String COLLECTION = "cupons";
    private static final String COLECAO_RECOMPENSAS = "recompensas";

    private final Firestore firestore;

    public CupomRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public Optional<Cupom> findByCodigoValidacao(String codigoValidacao) {
        QuerySnapshot snapshot = FirestoreUtils.await(
                firestore.collection(COLLECTION)
                        .whereEqualTo("codigoValidacao", codigoValidacao)
                        .limit(1)
                        .get());
        if (snapshot.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(snapshot.getDocuments().get(0).toObject(Cupom.class));
    }

    public List<Cupom> findByCiclistaIdOrderByDataResgateDesc(String ciclistaId) {
        QuerySnapshot snapshot = FirestoreUtils.await(
                firestore.collection(COLLECTION)
                        .whereEqualTo("ciclistaId", ciclistaId)
                        .orderBy("dataResgate", Query.Direction.DESCENDING)
                        .get());
        return snapshot.getDocuments().stream()
                .map(doc -> doc.toObject(Cupom.class))
                .toList();
    }

    /**
     * Valida o cupom de forma atômica: confere status e se a recompensa
     * pertence ao parceiro autenticado, depois marca como utilizado.
     */
    public ValidacaoResultado validar(String codigoValidacao, String parceiroId) {
        Optional<Cupom> encontrado = findByCodigoValidacao(codigoValidacao);
        if (encontrado.isEmpty()) {
            return ValidacaoResultado.erro(Status.NAO_ENCONTRADO);
        }

        DocumentReference cupomRef = firestore.collection(COLLECTION)
                .document(encontrado.get().getId());

        return FirestoreUtils.await(firestore.runTransaction(transaction -> {
            DocumentSnapshot cupomDoc = FirestoreUtils.await(transaction.get(cupomRef));
            if (!cupomDoc.exists()) {
                return ValidacaoResultado.erro(Status.NAO_ENCONTRADO);
            }

            Cupom cupom = cupomDoc.toObject(Cupom.class);
            StatusCupom status = cupom.getStatus() == null
                    ? StatusCupom.PENDENTE
                    : cupom.getStatus();

            if (status == StatusCupom.UTILIZADO) {
                return ValidacaoResultado.erro(Status.JA_UTILIZADO);
            }
            if (status == StatusCupom.EXPIRADO) {
                return ValidacaoResultado.erro(Status.EXPIRADO);
            }
            if (cupom.getRecompensaId() == null || cupom.getRecompensaId().isBlank()) {
                return ValidacaoResultado.erro(Status.RECOMPENSA_NAO_ENCONTRADA);
            }

            DocumentReference recompensaRef = firestore.collection(COLECAO_RECOMPENSAS)
                    .document(cupom.getRecompensaId());
            DocumentSnapshot recompensaDoc = FirestoreUtils.await(transaction.get(recompensaRef));
            if (!recompensaDoc.exists()) {
                return ValidacaoResultado.erro(Status.RECOMPENSA_NAO_ENCONTRADA);
            }

            Recompensa recompensa = recompensaDoc.toObject(Recompensa.class);
            if (recompensa.getParceiroId() == null
                    || !recompensa.getParceiroId().equals(parceiroId)) {
                return ValidacaoResultado.erro(Status.SEM_PERMISSAO);
            }

            cupom.setStatus(StatusCupom.UTILIZADO);
            cupom.setDataUtilizacao(new Date());
            transaction.set(cupomRef, cupom);

            return ValidacaoResultado.ok(cupom);
        }));
    }

    public Cupom save(Cupom cupom) {
        DocumentReference ref;
        if (cupom.getId() == null || cupom.getId().isBlank()) {
            ref = firestore.collection(COLLECTION).document();
            cupom.setId(ref.getId());
        } else {
            ref = firestore.collection(COLLECTION).document(cupom.getId());
        }
        FirestoreUtils.await(ref.set(cupom));
        return cupom;
    }
}
