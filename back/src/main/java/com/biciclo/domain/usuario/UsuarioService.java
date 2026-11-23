package com.biciclo.domain.usuario;

import com.biciclo.common.exception.ResourceNotFoundException;
import com.biciclo.domain.extrato.ExtratoPontosRepository;
import com.biciclo.domain.extrato.dto.ExtratoResponse;
import com.biciclo.domain.usuario.dto.UsuarioResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ExtratoPontosRepository extratoRepository;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          ExtratoPontosRepository extratoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.extratoRepository = extratoRepository;
    }

    public UsuarioResponse me(String usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .map(UsuarioResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    public List<ExtratoResponse> extrato(String usuarioId) {
        return extratoRepository.findByCiclistaIdOrderByCreatedAtDesc(usuarioId).stream()
                .map(ExtratoResponse::from)
                .toList();
    }
}
