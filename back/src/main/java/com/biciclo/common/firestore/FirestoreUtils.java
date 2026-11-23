package com.biciclo.common.firestore;

import com.google.api.core.ApiFuture;

import java.util.concurrent.ExecutionException;

/**
 * Utilitário para desbloquear ApiFuture do Firestore, convertendo as exceções
 * checadas em RuntimeException (tratadas pelo GlobalExceptionHandler como 500).
 */
public final class FirestoreUtils {

    private FirestoreUtils() {
    }

    public static <T> T await(ApiFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operação interrompida", e);
        } catch (ExecutionException e) {
            // Preserva exceções de domínio lançadas dentro de transações
            // (BusinessException, ResourceNotFoundException, etc.).
            Throwable causa = e.getCause();
            if (causa instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("Erro ao acessar o Firestore", causa);
        }
    }
}
