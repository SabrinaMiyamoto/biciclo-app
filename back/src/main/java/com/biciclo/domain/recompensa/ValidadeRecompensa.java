package com.biciclo.domain.recompensa;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Parsing/validação da data de expiração das recompensas.
 * Formatos aceitos: "yyyy-MM-dd", ISO-8601 com fuso (2026-12-31T23:59:59Z)
 * ou data/hora local (2026-12-31T23:59:59).
 */
final class ValidadeRecompensa {

    private ValidadeRecompensa() {
    }

    static boolean validadeVazia(String data) {
        return data == null || data.isBlank();
    }

    static boolean formatoValido(String data) {
        if (validadeVazia(data)) {
            return true;
        }
        try {
            if (data.length() == 10) {
                LocalDate.parse(data);
                return true;
            }
            try {
                Instant.parse(data);
                return true;
            } catch (DateTimeParseException e) {
                LocalDateTime.parse(data);
                return true;
            }
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    static boolean expirada(String data) {
        if (validadeVazia(data)) {
            return false;
        }
        try {
            if (data.length() == 10) {
                return LocalDate.parse(data).isBefore(LocalDate.now());
            }
            return Instant.parse(data).isBefore(Instant.now());
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(data).isBefore(LocalDateTime.now());
            } catch (DateTimeParseException ignored) {
                // Formato inválido não deve esconder a recompensa do catálogo.
                return false;
            }
        }
    }
}
