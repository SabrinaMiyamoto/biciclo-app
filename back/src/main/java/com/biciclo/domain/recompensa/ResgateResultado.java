package com.biciclo.domain.recompensa;

import com.biciclo.domain.cupom.Cupom;

/**
 * Resultado interno de um resgate. Evita lançar exceções dentro da transação
 * do Firestore: o service traduz o status em HTTP.
 */
record ResgateResultado(Status status, Cupom cupom) {

    enum Status {
        OK,
        RECOMPENSA_NAO_ENCONTRADA,
        USUARIO_NAO_ENCONTRADO,
        INATIVA,
        ESGOTADA,
        EXPIRADA,
        SALDO_INSUFICIENTE,
        DADOS_INVALIDOS
    }

    static ResgateResultado ok(Cupom cupom) {
        return new ResgateResultado(Status.OK, cupom);
    }

    static ResgateResultado erro(Status status) {
        return new ResgateResultado(status, null);
    }
}
