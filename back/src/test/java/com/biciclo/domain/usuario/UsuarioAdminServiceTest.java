package com.biciclo.domain.usuario;

import com.biciclo.common.enums.Role;
import com.biciclo.common.exception.BusinessException;
import com.biciclo.common.exception.ResourceNotFoundException;
import com.biciclo.domain.usuario.dto.UsuarioResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioAdminServiceTest {

    @Mock
    private UsuarioRepository repository;

    @InjectMocks
    private UsuarioAdminService service;

    @Test
    void promoveCiclistaAAdmin() {
        Usuario usuario = usuario("u1", Role.CYCLIST);
        when(repository.findById("u1")).thenReturn(Optional.of(usuario));

        UsuarioResponse response = service.alterarRole("u1", Role.ADMIN, "admin1");

        assertEquals(Role.ADMIN, response.role());
        verify(repository).save(usuario);
    }

    @Test
    void adminNaoAlteraOProprioPerfil() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.alterarRole("admin1", Role.CYCLIST, "admin1"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        verifyNoInteractions(repository);
    }

    @Test
    void naoTransformaUsuarioEmParceiro() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.alterarRole("u1", Role.PARTNER, "admin1"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void naoAlteraPerfilDeParceiro() {
        when(repository.findById("p1")).thenReturn(Optional.of(usuario("p1", Role.PARTNER)));

        assertThrows(BusinessException.class, () -> service.alterarRole("p1", Role.ADMIN, "admin1"));
        verify(repository, never()).save(any());
    }

    @Test
    void usuarioInexistenteDa404() {
        when(repository.findById("x")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.alterarRole("x", Role.ADMIN, "admin1"));
    }

    @Test
    void listaPorPerfil() {
        when(repository.findAll(Role.ADMIN, 100)).thenReturn(List.of(usuario("a", Role.ADMIN)));

        assertEquals(1, service.listar(Role.ADMIN).size());
    }

    private static Usuario usuario(String id, Role role) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail(id + "@biciclo.com");
        usuario.setRole(role);
        return usuario;
    }
}
