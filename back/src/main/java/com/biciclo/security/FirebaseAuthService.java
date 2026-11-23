package com.biciclo.security;

import com.biciclo.common.exception.BusinessException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Verifica tokens de ID emitidos pelo Firebase Auth (client-side).
 */
@Service
public class FirebaseAuthService {

    private final FirebaseAuth firebaseAuth;

    public FirebaseAuthService(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    public FirebaseToken verifyIdToken(String idToken) {
        try {
            return firebaseAuth.verifyIdToken(idToken);
        } catch (FirebaseAuthException | IllegalArgumentException e) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Token Firebase inválido ou expirado");
        }
    }
}
