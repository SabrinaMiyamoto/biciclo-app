package com.biciclo.domain.recompensa;

import com.biciclo.common.enums.Role;
import com.biciclo.common.exception.BusinessException;
import com.biciclo.common.exception.ResourceNotFoundException;
import com.biciclo.domain.cupom.dto.CupomResponse;
import com.biciclo.domain.recompensa.dto.RecompensaRequest;
import com.biciclo.domain.recompensa.dto.RecompensaResponse;
import com.biciclo.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecompensaService {

    private final RecompensaRepository repository;

    public RecompensaService(RecompensaRepository repository) {
        this.repository = repository;
    }

    /** Catálogo público: apenas recompensas ativas, com estoque e na validade. */
    public List<RecompensaResponse> listar() {
        return repository.findByAtivaTrue().stream()
                .filter(recompensa -> recompensa.getQuantidadeEstoque() != null
                        && recompensa.getQuantidadeEstoque() > 0)
                .filter(recompensa -> !recompensa.expirada())
                .map(RecompensaResponse::from)
                .toList();
    }

    public RecompensaResponse buscar(String id) {
        return repository.findById(id)
                .map(RecompensaResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Recompensa não encontrada"));
    }

    /** Recompensas do próprio parceiro, incluindo inativas e sem estoque. */
    public List<RecompensaResponse> minhas(UserPrincipal principal) {
        return repository.findByParceiroId(principal.id()).stream()
                .map(RecompensaResponse::from)
                .toList();
    }

    public RecompensaResponse criar(UserPrincipal principal, RecompensaRequest request) {
        validarDataExpiracao(request.getDataExpiracao());

        Recompensa recompensa = new Recompensa();
        recompensa.setParceiroId(resolverParceiroId(principal, request));
        aplicarDados(recompensa, request, Boolean.TRUE);

        return RecompensaResponse.from(repository.save(recompensa));
    }

    public RecompensaResponse atualizar(String id,
                                        UserPrincipal principal,
                                        RecompensaRequest request) {
        validarDataExpiracao(request.getDataExpiracao());

        Recompensa atual = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recompensa não encontrada"));
        verificarPropriedade(principal, atual);

        Recompensa dados = new Recompensa();
        aplicarDados(dados, request, atual.getAtiva());

        return repository.atualizar(id, dados)
                .map(RecompensaResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Recompensa não encontrada"));
    }

    /** Soft delete: desativa a recompensa, preservando cupons já emitidos. */
    public void desativar(String id, UserPrincipal principal) {
        Recompensa atual = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recompensa não encontrada"));
        verificarPropriedade(principal, atual);

        repository.desativar(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recompensa não encontrada"));
    }

    public CupomResponse resgatar(String recompensaId, String ciclistaId) {
        ResgateResultado resultado = repository.resgatar(recompensaId, ciclistaId);

        return switch (resultado.status()) {
            case OK -> CupomResponse.from(resultado.cupom());
            case RECOMPENSA_NAO_ENCONTRADA ->
                    throw new ResourceNotFoundException("Recompensa não encontrada");
            case USUARIO_NAO_ENCONTRADO ->
                    throw new ResourceNotFoundException("Usuário não encontrado");
            case INATIVA ->
                    throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "Recompensa inativa");
            case ESGOTADA ->
                    throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "Estoque esgotado");
            case EXPIRADA ->
                    throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "Recompensa expirada");
            case SALDO_INSUFICIENTE ->
                    throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "Saldo insuficiente");
            case DADOS_INVALIDOS ->
                    throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Recompensa com dados inválidos");
        };
    }

    private String resolverParceiroId(UserPrincipal principal, RecompensaRequest request) {
        String informado = request.getParceiroId();
        if (informado != null && !informado.isBlank()) {
            String normalizado = informado.trim();
            if (principal.role() != Role.ADMIN && !principal.id().equals(normalizado)) {
                throw new BusinessException(HttpStatus.FORBIDDEN,
                        "Somente ADMIN pode definir outro parceiro");
            }
            return normalizado;
        }
        return principal.id();
    }

    private void aplicarDados(Recompensa recompensa,
                              RecompensaRequest request,
                              Boolean ativaPadrao) {
        recompensa.setTitulo(request.getTitulo().trim());
        recompensa.setDescricao(request.getDescricao());
        recompensa.setCustoPontos(request.getCustoPontos());
        recompensa.setQuantidadeEstoque(request.getQuantidadeEstoque());
        recompensa.setDataExpiracao(normalizarData(request.getDataExpiracao()));

        if (request.getAtiva() != null) {
            recompensa.setAtiva(request.getAtiva());
        } else {
            recompensa.setAtiva(ativaPadrao != null ? ativaPadrao : Boolean.TRUE);
        }
    }

    private String normalizarData(String data) {
        if (data == null || data.isBlank()) {
            return null;
        }
        return data.trim();
    }

    private void validarDataExpiracao(String data) {
        if (!ValidadeRecompensa.formatoValido(data)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "dataExpiracao inválida. Use yyyy-MM-dd ou ISO-8601");
        }
    }

    private void verificarPropriedade(UserPrincipal principal, Recompensa recompensa) {
        if (principal.role() != Role.ADMIN
                && !principal.id().equals(recompensa.getParceiroId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Recompensa pertence a outro parceiro");
        }
    }
}
