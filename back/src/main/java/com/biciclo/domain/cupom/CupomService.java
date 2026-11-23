package com.biciclo.domain.cupom;

import com.biciclo.common.exception.BusinessException;
import com.biciclo.common.exception.ResourceNotFoundException;
import com.biciclo.domain.cupom.dto.CupomResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CupomService {

    private final CupomRepository repository;

    public CupomService(CupomRepository repository) {
        this.repository = repository;
    }

    public List<CupomResponse> meus(String ciclistaId) {
        return repository.findByCiclistaIdOrderByDataResgateDesc(ciclistaId).stream()
                .map(CupomResponse::from)
                .toList();
    }

    public CupomResponse validar(String codigo, String parceiroId) {
        if (codigo == null || codigo.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Código do cupom é obrigatório");
        }

        ValidacaoResultado resultado = repository.validar(codigo.trim(), parceiroId);

        return switch (resultado.status()) {
            case OK -> CupomResponse.from(resultado.cupom());
            case NAO_ENCONTRADO ->
                    throw new ResourceNotFoundException("Cupom não encontrado");
            case JA_UTILIZADO ->
                    throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "Cupom já utilizado");
            case EXPIRADO ->
                    throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "Cupom expirado");
            case SEM_PERMISSAO ->
                    throw new BusinessException(HttpStatus.FORBIDDEN,
                            "Cupom não pertence a este parceiro");
            case RECOMPENSA_NAO_ENCONTRADA ->
                    throw new ResourceNotFoundException("Recompensa do cupom não encontrada");
        };
    }
}
