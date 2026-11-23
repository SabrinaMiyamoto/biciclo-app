package com.biciclo.domain.recompensa;

import com.biciclo.common.enums.StatusCupom;
import com.biciclo.common.enums.TipoMovimento;
import com.biciclo.common.enums.TipoReferencia;
import com.biciclo.common.firestore.FirestoreUtils;
import com.biciclo.domain.cupom.Cupom;
import com.biciclo.domain.extrato.ExtratoPontos;
import com.biciclo.domain.recompensa.ResgateResultado.Status;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public class RecompensaRepository {

    private static final String COLLECTION = "recompensas";
    private static final String COLECAO_USUARIOS = "usuarios";
    private static final String COLECAO_CUPONS = "cupons";
    private static final String COLECAO_EXTRATO = "extrato_pontos";

    private final Firestore firestore;

    public RecompensaRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public Recompensa save(Recompensa recompensa) {
        DocumentReference ref;
        if (recompensa.getId() == null || recompensa.getId().isBlank()) {
            ref = firestore.collection(COLLECTION).document();
            recompensa.setId(ref.getId());
        } else {
            ref = firestore.collection(COLLECTION).document(recompensa.getId());
        }
        FirestoreUtils.await(ref.set(recompensa));
        return recompensa;
    }

    public List<Recompensa> findByAtivaTrue() {
        QuerySnapshot snapshot = FirestoreUtils.await(
                firestore.collection(COLLECTION)
                        .whereEqualTo("ativa", true)
                        .get());
        return snapshot.getDocuments().stream()
                .map(doc -> doc.toObject(Recompensa.class))
                .toList();
    }

    public Optional<Recompensa> findById(String id) {
        DocumentSnapshot doc = FirestoreUtils.await(
                firestore.collection(COLLECTION).document(id).get());
        if (!doc.exists()) {
            return Optional.empty();
        }
        return Optional.of(doc.toObject(Recompensa.class));
    }

    public List<Recompensa> findByParceiroId(String parceiroId) {
        QuerySnapshot snapshot = FirestoreUtils.await(
                firestore.collection(COLLECTION)
                        .whereEqualTo("parceiroId", parceiroId)
                        .get());
        return snapshot.getDocuments().stream()
                .map(doc -> doc.toObject(Recompensa.class))
                .toList();
    }

    /**
     * Atualiza apenas os campos editáveis, dentro de uma transação, para não
     * sobrescrever alterações concorrentes de estoque feitas por um resgate.
     * O {@code parceiroId} nunca é alterado.
     */
    public Optional<Recompensa> atualizar(String id, Recompensa dados) {
        DocumentReference ref = firestore.collection(COLLECTION).document(id);

        return FirestoreUtils.await(firestore.runTransaction(transaction -> {
            DocumentSnapshot doc = FirestoreUtils.await(transaction.get(ref));
            if (!doc.exists()) {
                return Optional.<Recompensa>empty();
            }

            Recompensa atual = doc.toObject(Recompensa.class);
            atual.setTitulo(dados.getTitulo());
            atual.setDescricao(dados.getDescricao());
            atual.setCustoPontos(dados.getCustoPontos());
            atual.setQuantidadeEstoque(dados.getQuantidadeEstoque());
            atual.setDataExpiracao(dados.getDataExpiracao());
            atual.setAtiva(dados.getAtiva());

            transaction.set(ref, atual);
            return Optional.of(atual);
        }));
    }

    /**
     * Soft delete: desativa a recompensa em vez de excluí-la, preservando os
     * cupons já emitidos que a referenciam.
     */
    public Optional<Recompensa> desativar(String id) {
        DocumentReference ref = firestore.collection(COLLECTION).document(id);

        return FirestoreUtils.await(firestore.runTransaction(transaction -> {
            DocumentSnapshot doc = FirestoreUtils.await(transaction.get(ref));
            if (!doc.exists()) {
                return Optional.<Recompensa>empty();
            }

            Recompensa atual = doc.toObject(Recompensa.class);
            atual.setAtiva(false);

            transaction.set(ref, atual);
            return Optional.of(atual);
        }));
    }

    /**
     * Resgata uma recompensa de forma atômica: valida estoque/saldo e, se tudo
     * estiver ok, baixa o estoque, debita os pontos, cria o cupom e registra a
     * saída no extrato na mesma transação (RNF01).
     */
    public ResgateResultado resgatar(String recompensaId, String ciclistaId) {
        DocumentReference recompensaRef = firestore.collection(COLLECTION).document(recompensaId);
        DocumentReference usuarioRef = firestore.collection(COLECAO_USUARIOS).document(ciclistaId);

        return FirestoreUtils.await(firestore.runTransaction(transaction -> {
            DocumentSnapshot recompensaDoc = FirestoreUtils.await(transaction.get(recompensaRef));
            if (!recompensaDoc.exists()) {
                return ResgateResultado.erro(Status.RECOMPENSA_NAO_ENCONTRADA);
            }

            Recompensa recompensa = recompensaDoc.toObject(Recompensa.class);
            if (!Boolean.TRUE.equals(recompensa.getAtiva())) {
                return ResgateResultado.erro(Status.INATIVA);
            }
            if (recompensa.getQuantidadeEstoque() == null || recompensa.getQuantidadeEstoque() <= 0) {
                return ResgateResultado.erro(Status.ESGOTADA);
            }
            if (recompensa.expirada()) {
                return ResgateResultado.erro(Status.EXPIRADA);
            }
            if (recompensa.getCustoPontos() == null || recompensa.getCustoPontos() < 0) {
                return ResgateResultado.erro(Status.DADOS_INVALIDOS);
            }

            DocumentSnapshot usuarioDoc = FirestoreUtils.await(transaction.get(usuarioRef));
            if (!usuarioDoc.exists()) {
                return ResgateResultado.erro(Status.USUARIO_NAO_ENCONTRADO);
            }

            Long saldoDoc = usuarioDoc.getLong("saldoPontos");
            int saldo = saldoDoc == null ? 0 : saldoDoc.intValue();
            int custo = recompensa.getCustoPontos();
            if (saldo < custo) {
                return ResgateResultado.erro(Status.SALDO_INSUFICIENTE);
            }

            Date agora = new Date();
            DocumentReference cupomRef = firestore.collection(COLECAO_CUPONS).document();

            Cupom cupom = new Cupom();
            cupom.setId(cupomRef.getId());
            cupom.setCodigoValidacao(cupomRef.getId());
            cupom.setRecompensaId(recompensaId);
            cupom.setCiclistaId(ciclistaId);
            cupom.setStatus(StatusCupom.PENDENTE);
            cupom.setDataResgate(agora);

            ExtratoPontos extrato = new ExtratoPontos();
            extrato.setCiclistaId(ciclistaId);
            extrato.setTipoMovimento(TipoMovimento.SAIDA);
            extrato.setQuantidade(custo);
            extrato.setReferenciaTipo(TipoReferencia.CUPOM);
            extrato.setReferenciaId(cupom.getId());
            extrato.setCreatedAt(agora);
            DocumentReference extratoRef = firestore.collection(COLECAO_EXTRATO).document();
            extrato.setId(extratoRef.getId());

            transaction.update(recompensaRef, "quantidadeEstoque",
                    recompensa.getQuantidadeEstoque() - 1);
            transaction.update(usuarioRef, "saldoPontos", saldo - custo);
            transaction.set(extratoRef, extrato);
            transaction.set(cupomRef, cupom);

            return ResgateResultado.ok(cupom);
        }));
    }
}
