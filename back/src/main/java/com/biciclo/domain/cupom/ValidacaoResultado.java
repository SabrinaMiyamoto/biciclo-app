package com.biciclo.domain.cupom;

/**
 * Resultado interno da validação de um cupom por um parceiro.
 */
record ValidacaoResultado(Status status, Cupom cupom) {

    enum Status {
        OK,
        NAO_ENCONTRADO,
        JA_UTILIZADO,
        EXPIRADO,
        SEM_PERMISSAO,
        RECOMPENSA_NAO_ENCONTRADA
    }

    static ValidacaoResultado ok(Cupom cupom) {
        return new ValidacaoResultado(Status.OK, cupom);
    }

    static ValidacaoResultado erro(Status status) {
        return new ValidacaoResultado(status, null);
    }
}
