package com.biciclo.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Inicializa o Firebase Admin SDK e expõe o cliente Firestore.
 *
 * A credencial pode vir de um arquivo JSON de conta de serviço
 * (app.firebase.service-account-path) ou, quando ausente, das credenciais
 * padrão do ambiente (GOOGLE_APPLICATION_CREDENTIALS / Cloud Run).
 */
@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp(FirebaseProperties properties) throws IOException {
        FirebaseOptions.Builder builder = FirebaseOptions.builder();

        String caminho = properties.serviceAccountPath();
        if (caminho != null && !caminho.isBlank()) {
            Path arquivo = Path.of(caminho);
            if (!Files.isReadable(arquivo)) {
                throw new IllegalStateException(
                        "Credencial do Firebase não encontrada ou sem permissão de leitura: " + caminho);
            }
            try (InputStream serviceAccount = Files.newInputStream(arquivo)) {
                builder.setCredentials(GoogleCredentials.fromStream(serviceAccount));
            }
        } else {
            try {
                builder.setCredentials(GoogleCredentials.getApplicationDefault());
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Credenciais do Firebase não configuradas. Defina a variável "
                        + "FIREBASE_SERVICE_ACCOUNT_PATH apontando para o JSON da conta de serviço "
                        + "ou GOOGLE_APPLICATION_CREDENTIALS.", e);
            }
        }

        return FirebaseApp.initializeApp(builder.build());
    }

    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) {
        return FirestoreClient.getFirestore(firebaseApp);
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }
}
