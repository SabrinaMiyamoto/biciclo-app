package com.biciclo.domain.usuario;

import com.biciclo.common.firestore.FirestoreUtils;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.biciclo.common.enums.Role;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UsuarioRepository {

    private static final String COLLECTION = "usuarios";

    private final Firestore firestore;

    public UsuarioRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public Optional<Usuario> findByEmail(String email) {
        QuerySnapshot snapshot = FirestoreUtils.await(
                firestore.collection(COLLECTION)
                        .whereEqualTo("email", email)
                        .limit(1)
                        .get());
        if (snapshot.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(snapshot.getDocuments().get(0).toObject(Usuario.class));
    }

    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    public Optional<Usuario> findById(String id) {
        DocumentSnapshot doc = FirestoreUtils.await(
                firestore.collection(COLLECTION).document(id).get());
        if (!doc.exists()) {
            return Optional.empty();
        }
        return Optional.of(doc.toObject(Usuario.class));
    }

    public Optional<Usuario> findByFirebaseUid(String firebaseUid) {
        QuerySnapshot snapshot = FirestoreUtils.await(
                firestore.collection(COLLECTION)
                        .whereEqualTo("firebaseUid", firebaseUid)
                        .limit(1)
                        .get());
        if (snapshot.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(snapshot.getDocuments().get(0).toObject(Usuario.class));
    }

    public List<Usuario> findAll(Role role, int limite) {
        Query query = firestore.collection(COLLECTION);
        if (role != null) {
            query = query.whereEqualTo("role", role);
        }
        QuerySnapshot snapshot = FirestoreUtils.await(query.limit(limite).get());
        return snapshot.getDocuments().stream()
                .map(doc -> doc.toObject(Usuario.class))
                .toList();
    }

    public Usuario save(Usuario usuario) {
        DocumentReference ref;
        if (usuario.getId() == null || usuario.getId().isBlank()) {
            ref = firestore.collection(COLLECTION).document();
            usuario.setId(ref.getId());
        } else {
            ref = firestore.collection(COLLECTION).document(usuario.getId());
        }
        FirestoreUtils.await(ref.set(usuario));
        return usuario;
    }
}
