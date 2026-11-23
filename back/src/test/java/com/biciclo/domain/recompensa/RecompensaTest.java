package com.biciclo.domain.recompensa;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecompensaTest {

    @Test
    void semDataNaoExpira() {
        assertFalse(recompensaCom(null).expirada());
        assertFalse(recompensaCom("  ").expirada());
    }

    @Test
    void dataPassadaExpira() {
        String ontem = LocalDate.now().minusDays(1).toString();
        assertTrue(recompensaCom(ontem).expirada());
    }

    @Test
    void dataFuturaNaoExpira() {
        String amanha = LocalDate.now().plusDays(1).toString();
        assertFalse(recompensaCom(amanha).expirada());
    }

    @Test
    void aceitaIsoComFuso() {
        assertTrue(recompensaCom("2020-01-01T10:00:00Z").expirada());
        assertFalse(recompensaCom("2999-01-01T10:00:00Z").expirada());
    }

    @Test
    void formatoInvalidoNaoBloqueia() {
        assertFalse(recompensaCom("31/12/2026").expirada());
    }

    @Test
    void validaFormatosAceitos() {
        assertTrue(ValidadeRecompensa.formatoValido("2026-12-31"));
        assertTrue(ValidadeRecompensa.formatoValido("2026-12-31T23:59:59Z"));
        assertTrue(ValidadeRecompensa.formatoValido("2026-12-31T23:59:59"));
        assertTrue(ValidadeRecompensa.formatoValido(null));
        assertFalse(ValidadeRecompensa.formatoValido("31/12/2026"));
    }

    private static Recompensa recompensaCom(String dataExpiracao) {
        Recompensa recompensa = new Recompensa();
        recompensa.setDataExpiracao(dataExpiracao);
        return recompensa;
    }
}
