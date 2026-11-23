package com.biciclo.domain.usuario;

import com.biciclo.common.enums.Role;
import com.biciclo.common.enums.TipoMovimento;
import com.biciclo.common.enums.TipoReferencia;
import com.biciclo.common.exception.ResourceNotFoundException;
import com.biciclo.domain.extrato.ExtratoPontos;
import com.biciclo.domain.extrato.ExtratoPontosRepository;
import com.biciclo.domain.extrato.dto.ExtratoResponse;
import com.biciclo.domain.usuario.dto.UsuarioResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ExtratoPontosRepository extratoRepository;

    @InjectMocks
    private UsuarioService service;

    @Test
    void retornaPerfilDoUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId("u1");
        usuario.setNome("Ana");
        usuario.setEmail("ana@biciclo.com");
        usuario.setRole(Role.CYCLIST);
        usuario.setSaldoPontos(11);
        usuario.setCreatedAt(new Date());
        when(usuarioRepository.findById("u1")).thenReturn(Optional.of(usuario));

        UsuarioResponse response = service.me("u1");

        assertEquals("u1", response.id());
        assertEquals("Ana", response.nome());
        assertEquals(11, response.saldoPontos());
    }

    @Test
    void perfilInexistenteRetorna404() {
        when(usuarioRepository.findById("u1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.me("u1"));
    }

    @Test
    void retornaExtratoDoUsuario() {
        ExtratoPontos extrato = new ExtratoPontos();
        extrato.setId("e1");
        extrato.setTipoMovimento(TipoMovimento.ENTRADA);
        extrato.setQuantidade(11);
        extrato.setReferenciaTipo(TipoReferencia.TRAJETO);
        extrato.setReferenciaId("t1");
        extrato.setCreatedAt(new Date());
        when(extratoRepository.findByCiclistaIdOrderByCreatedAtDesc("u1"))
                .thenReturn(List.of(extrato));

        List<ExtratoResponse> response = service.extrato("u1");

        assertEquals(1, response.size());
        assertEquals(11, response.get(0).quantidade());
        assertEquals(TipoMovimento.ENTRADA, response.get(0).tipoMovimento());
    }
}
