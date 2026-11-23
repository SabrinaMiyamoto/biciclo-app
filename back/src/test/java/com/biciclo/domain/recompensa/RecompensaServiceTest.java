package com.biciclo.domain.recompensa;

import com.biciclo.common.enums.Role;
import com.biciclo.common.enums.StatusCupom;
import com.biciclo.common.exception.BusinessException;
import com.biciclo.common.exception.ResourceNotFoundException;
import com.biciclo.domain.cupom.Cupom;
import com.biciclo.domain.cupom.dto.CupomResponse;
import com.biciclo.domain.recompensa.dto.RecompensaRequest;
import com.biciclo.domain.recompensa.dto.RecompensaResponse;
import com.biciclo.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecompensaServiceTest {

    @Mock
    private RecompensaRepository repository;

    @InjectMocks
    private RecompensaService service;

    @Test
    void listaSomenteRecompensasComEstoqueENaValidade() {
        Recompensa valida = recompensa("r1", 11, 5, null);
        Recompensa semEstoque = recompensa("r2", 11, 0, null);
        Recompensa expirada = recompensa("r3", 11, 5, LocalDate.now().minusDays(1).toString());

        when(repository.findByAtivaTrue()).thenReturn(List.of(valida, semEstoque, expirada));

        List<RecompensaResponse> response = service.listar();

        assertEquals(1, response.size());
        assertEquals("r1", response.get(0).id());
        assertEquals(11, response.get(0).custoPontos());
    }

    @Test
    void criaRecompensaUsandoParceiroDoPrincipal() {
        when(repository.save(any(Recompensa.class))).thenAnswer(invocation -> {
            Recompensa salva = invocation.getArgument(0);
            salva.setId("r1");
            return salva;
        });

        RecompensaResponse response = service.criar(
                principal("p1", Role.PARTNER),
                request("10% na oficina", 11, 5, null));

        ArgumentCaptor<Recompensa> captor = ArgumentCaptor.forClass(Recompensa.class);
        verify(repository).save(captor.capture());
        Recompensa salva = captor.getValue();

        assertEquals("p1", salva.getParceiroId());
        assertEquals(Boolean.TRUE, salva.getAtiva());
        assertEquals("r1", response.id());
        assertEquals(11, response.custoPontos());
    }

    @Test
    void adminPodeCriarParaOutroParceiro() {
        when(repository.save(any(Recompensa.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RecompensaRequest request = request("Cupom do parceiro", 10, 2, null);
        request.setParceiroId("p2");

        service.criar(principal("admin-1", Role.ADMIN), request);

        ArgumentCaptor<Recompensa> captor = ArgumentCaptor.forClass(Recompensa.class);
        verify(repository).save(captor.capture());
        assertEquals("p2", captor.getValue().getParceiroId());
    }

    @Test
    void parceiroNaoPodeCriarParaOutroParceiro() {
        RecompensaRequest request = request("Cupom", 10, 2, null);
        request.setParceiroId("p2");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.criar(principal("p1", Role.PARTNER), request));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void rejeitaDataDeExpiracaoInvalida() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.criar(principal("p1", Role.PARTNER),
                        request("Cupom", 10, 2, "31/12/2026")));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void minhasListaRecompensasDoParceiro() {
        when(repository.findByParceiroId("p1"))
                .thenReturn(List.of(recompensa("r1", 11, 5, null)));

        List<RecompensaResponse> response = service.minhas(principal("p1", Role.PARTNER));

        assertEquals(1, response.size());
        assertEquals("r1", response.get(0).id());
    }

    @Test
    void atualizaRecompensaDoProprioParceiro() {
        Recompensa existente = recompensa("r1", 11, 5, null);
        existente.setParceiroId("p1");

        when(repository.findById("r1")).thenReturn(Optional.of(existente));
        when(repository.atualizar(eq("r1"), any(Recompensa.class))).thenAnswer(invocation -> {
            Recompensa dados = invocation.getArgument(1);
            dados.setId("r1");
            dados.setParceiroId("p1");
            return Optional.of(dados);
        });

        RecompensaResponse response = service.atualizar(
                "r1", principal("p1", Role.PARTNER),
                request("Novo título", 20, 3, null));

        assertEquals("Novo título", response.titulo());
        assertEquals(20, response.custoPontos());
        assertEquals(3, response.quantidadeEstoque());
        assertEquals(Boolean.TRUE, response.ativa());
    }

    @Test
    void naoAtualizaRecompensaDeOutroParceiro() {
        Recompensa existente = recompensa("r1", 11, 5, null);
        existente.setParceiroId("p2");

        when(repository.findById("r1")).thenReturn(Optional.of(existente));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.atualizar("r1", principal("p1", Role.PARTNER),
                        request("Invadindo", 1, 1, null)));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(repository, never()).atualizar(anyString(), any());
    }

    @Test
    void desativaRecompensaDoProprioParceiro() {
        Recompensa existente = recompensa("r1", 11, 5, null);
        existente.setParceiroId("p1");

        when(repository.findById("r1")).thenReturn(Optional.of(existente));
        when(repository.desativar("r1")).thenReturn(Optional.of(existente));

        service.desativar("r1", principal("p1", Role.PARTNER));

        verify(repository).desativar("r1");
    }

    @Test
    void resgataComSucesso() {
        when(repository.resgatar("r1", "u1"))
                .thenReturn(ResgateResultado.ok(cupom(StatusCupom.PENDENTE)));

        CupomResponse response = service.resgatar("r1", "u1");

        assertEquals("cupom-1", response.id());
        assertEquals("CODIGO123", response.codigoValidacao());
        assertEquals(StatusCupom.PENDENTE, response.status());
    }

    @Test
    void saldoInsuficienteRetorna422() {
        when(repository.resgatar("r1", "u1"))
                .thenReturn(ResgateResultado.erro(ResgateResultado.Status.SALDO_INSUFICIENTE));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.resgatar("r1", "u1"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        assertEquals("Saldo insuficiente", ex.getMessage());
    }

    @Test
    void estoqueEsgotadoRetorna422() {
        when(repository.resgatar("r1", "u1"))
                .thenReturn(ResgateResultado.erro(ResgateResultado.Status.ESGOTADA));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.resgatar("r1", "u1"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        assertEquals("Estoque esgotado", ex.getMessage());
    }

    @Test
    void recompensaNaoEncontradaRetorna404() {
        when(repository.resgatar("r1", "u1"))
                .thenReturn(ResgateResultado.erro(ResgateResultado.Status.RECOMPENSA_NAO_ENCONTRADA));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.resgatar("r1", "u1"));

        assertEquals("Recompensa não encontrada", ex.getMessage());
    }

    private static RecompensaRequest request(String titulo, int custo, int estoque, String expiracao) {
        RecompensaRequest request = new RecompensaRequest();
        request.setTitulo(titulo);
        request.setCustoPontos(custo);
        request.setQuantidadeEstoque(estoque);
        request.setDataExpiracao(expiracao);
        return request;
    }

    private static UserPrincipal principal(String id, Role role) {
        return new UserPrincipal(id, id + "@teste.com", role);
    }

    private static Recompensa recompensa(String id, int custo, int estoque, String expiracao) {
        Recompensa recompensa = new Recompensa();
        recompensa.setId(id);
        recompensa.setTitulo("Recompensa " + id);
        recompensa.setCustoPontos(custo);
        recompensa.setQuantidadeEstoque(estoque);
        recompensa.setDataExpiracao(expiracao);
        recompensa.setAtiva(true);
        return recompensa;
    }

    private static Cupom cupom(StatusCupom status) {
        Cupom cupom = new Cupom();
        cupom.setId("cupom-1");
        cupom.setCodigoValidacao("CODIGO123");
        cupom.setRecompensaId("r1");
        cupom.setCiclistaId("u1");
        cupom.setStatus(status);
        cupom.setDataResgate(new Date());
        return cupom;
    }
}
