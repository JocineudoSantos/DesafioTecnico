package br.com.dunnastecnologia.chamados.unit.service;

import br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser;
import br.com.dunnastecnologia.chamados.domain.model.Chamado;
import br.com.dunnastecnologia.chamados.domain.model.Morador;
import br.com.dunnastecnologia.chamados.domain.model.StatusChamado;
import br.com.dunnastecnologia.chamados.domain.model.TipoChamado;
import br.com.dunnastecnologia.chamados.domain.model.Unidade;
import br.com.dunnastecnologia.chamados.infrastructure.exception.BusinessRuleException;
import br.com.dunnastecnologia.chamados.infrastructure.repository.ChamadoRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.MoradorRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.StatusChamadoRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.TipoChamadoRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.UnidadeRepository;
import br.com.dunnastecnologia.chamados.infrastructure.service.support.AuthenticatedUserValidator;
import br.com.dunnastecnologia.chamados.infrastructure.service.ChamadoService;
import br.com.dunnastecnologia.chamados.domain.validation.ValidationLimits;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChamadoServiceTest {

    @Mock
    private ChamadoRepository chamadoRepository;
    @Mock
    private MoradorRepository moradorRepository;
    @Mock
    private UnidadeRepository unidadeRepository;
    @Mock
    private TipoChamadoRepository tipoChamadoRepository;
    @Mock
    private StatusChamadoRepository statusChamadoRepository;
    @Mock
    private AuthenticatedUserValidator authenticatedUserValidator;

    @InjectMocks
    private ChamadoService chamadoService;

    @Test
    void abrirChamadoDeveCriarChamadoComStatusInicialPadrao() {
        UUID moradorId = UUID.randomUUID();
        UUID unidadeId = UUID.randomUUID();
        UUID tipoChamadoId = UUID.randomUUID();
        AuthenticatedUser moradorAutenticado = new AuthenticatedUser(moradorId, "morador@cond.local", "ROLE_MORADOR");

        Morador morador = new Morador();
        morador.setId(moradorId);

        Unidade unidade = new Unidade();
        unidade.setId(unidadeId);

        TipoChamado tipoChamado = new TipoChamado();
        tipoChamado.setId(tipoChamadoId);

        StatusChamado statusInicial = new StatusChamado();
        statusInicial.setNome("Solicitado");
        statusInicial.setInicialPadrao(Boolean.TRUE);

        when(moradorRepository.existsByIdAndUnidadeId(moradorId, unidadeId)).thenReturn(true);
        when(moradorRepository.findByIdAndAtivoTrue(moradorId)).thenReturn(Optional.of(morador));
        when(unidadeRepository.findById(unidadeId)).thenReturn(Optional.of(unidade));
        when(tipoChamadoRepository.findById(tipoChamadoId)).thenReturn(Optional.of(tipoChamado));
        when(statusChamadoRepository.findByInicialPadraoTrue()).thenReturn(Optional.of(statusInicial));
        when(chamadoRepository.save(any(Chamado.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Chamado chamado = chamadoService.abrirChamado(moradorAutenticado, unidadeId, tipoChamadoId, "Vazamento");

        assertEquals("Vazamento", chamado.getDescricao());
        assertEquals(morador, chamado.getMorador());
        assertEquals(unidade, chamado.getUnidade());
        assertEquals(tipoChamado, chamado.getTipoChamado());
        assertEquals(statusInicial, chamado.getStatus());
        assertNotNull(chamado.getDataAbertura());
    }

    @Test
    void abrirChamadoDeveFalharQuandoMoradorNaoTemAcessoAUnidade() {
        UUID moradorId = UUID.randomUUID();
        UUID unidadeId = UUID.randomUUID();
        UUID tipoChamadoId = UUID.randomUUID();
        AuthenticatedUser moradorAutenticado = new AuthenticatedUser(moradorId, "morador@cond.local", "ROLE_MORADOR");

        when(moradorRepository.existsByIdAndUnidadeId(moradorId, unidadeId)).thenReturn(false);

        assertThrows(
                BusinessRuleException.class,
                () -> chamadoService.abrirChamado(moradorAutenticado, unidadeId, tipoChamadoId, "Descricao")
        );
    }

    @Test
    void abrirChamadoDeveFalharQuandoDescricaoExcederLimite() {
        UUID moradorId = UUID.randomUUID();
        AuthenticatedUser moradorAutenticado = new AuthenticatedUser(moradorId, "morador@cond.local", "ROLE_MORADOR");

        assertThrows(
                BusinessRuleException.class,
                () -> chamadoService.abrirChamado(
                        moradorAutenticado,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "a".repeat(ValidationLimits.CHAMADO_DESCRICAO_MAX_LENGTH + 1)
                )
        );
    }

    @Test
    void atualizarStatusComoColaboradorDeveAplicarStatusFinalizadoEDataFinalizacao() {
        UUID colaboradorId = UUID.randomUUID();
        UUID chamadoId = UUID.randomUUID();
        UUID statusId = UUID.randomUUID();
        AuthenticatedUser colaborador = new AuthenticatedUser(colaboradorId, "colab@cond.local", "ROLE_COLABORADOR");

        Chamado chamado = new Chamado();
        chamado.setId(chamadoId);
        chamado.setDataFinalizacao(null);

        StatusChamado statusFinalizado = new StatusChamado();
        statusFinalizado.setId(statusId);
        statusFinalizado.setNome("Finalizado");

        when(chamadoRepository.findByIdAndColaboradorId(colaboradorId, chamadoId)).thenReturn(Optional.of(chamado));
        when(statusChamadoRepository.findById(statusId)).thenReturn(Optional.of(statusFinalizado));
        when(chamadoRepository.save(any(Chamado.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Chamado chamadoFinalizado = chamadoService.atualizarStatusComoColaborador(colaborador, chamadoId, statusId);

        assertEquals(statusFinalizado, chamadoFinalizado.getStatus());
        assertNotNull(chamadoFinalizado.getDataFinalizacao());
        assertTrue(chamadoFinalizado.getDataFinalizacao().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void atualizarStatusComoAdminDeveDefinirDataFinalizacaoQuandoNovoStatusForFinalizado() {
        UUID adminId = UUID.randomUUID();
        UUID chamadoId = UUID.randomUUID();
        UUID statusId = UUID.randomUUID();
        AuthenticatedUser admin = new AuthenticatedUser(adminId, "admin@cond.local", "ROLE_ADMINISTRADOR");

        Chamado chamado = new Chamado();
        chamado.setId(chamadoId);
        chamado.setDataFinalizacao(null);

        StatusChamado statusFinalizado = new StatusChamado();
        statusFinalizado.setId(statusId);
        statusFinalizado.setNome("Finalizado");

        when(chamadoRepository.findByIdAndAdminId(adminId, chamadoId)).thenReturn(Optional.of(chamado));
        when(statusChamadoRepository.findById(statusId)).thenReturn(Optional.of(statusFinalizado));
        when(chamadoRepository.save(any(Chamado.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Chamado chamadoFinalizado = chamadoService.atualizarStatusComoAdmin(admin, chamadoId, statusId);

        assertEquals(statusFinalizado, chamadoFinalizado.getStatus());
        assertNotNull(chamadoFinalizado.getDataFinalizacao());
        assertTrue(chamadoFinalizado.getDataFinalizacao().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void reabrirComoMoradorDeveLimparDataFinalizacaoEVoltarParaSolicitado() {
        UUID moradorId = UUID.randomUUID();
        UUID chamadoId = UUID.randomUUID();
        AuthenticatedUser morador = new AuthenticatedUser(moradorId, "morador@cond.local", "ROLE_MORADOR");

        StatusChamado statusSolicitado = new StatusChamado();
        statusSolicitado.setNome("Solicitado");

        Chamado chamado = new Chamado();
        chamado.setId(chamadoId);
        chamado.setDataAbertura(LocalDateTime.now().minusHours(1));
        chamado.setDataFinalizacao(LocalDateTime.now().minusHours(1));
        TipoChamado tipoChamado = new TipoChamado();
        tipoChamado.setPrazoHoras(4);
        chamado.setTipoChamado(tipoChamado);

        when(chamadoRepository.findByIdAndMoradorId(chamadoId, moradorId)).thenReturn(Optional.of(chamado));
        when(statusChamadoRepository.findByNome("Solicitado")).thenReturn(Optional.of(statusSolicitado));
        when(chamadoRepository.save(any(Chamado.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Chamado reaberto = chamadoService.reabrirComoMorador(morador, chamadoId);

        assertEquals(statusSolicitado, reaberto.getStatus());
        assertNull(reaberto.getDataFinalizacao());
    }

    @Test
    void reabrirComoMoradorDeveAplicarStatusAtrasadoQuandoSlaJaEstiverExpirado() {
        UUID moradorId = UUID.randomUUID();
        UUID chamadoId = UUID.randomUUID();
        AuthenticatedUser morador = new AuthenticatedUser(moradorId, "morador@cond.local", "ROLE_MORADOR");

        StatusChamado statusAtrasado = new StatusChamado();
        statusAtrasado.setNome("Atrasado");

        TipoChamado tipoChamado = new TipoChamado();
        tipoChamado.setPrazoHoras(2);

        Chamado chamado = new Chamado();
        chamado.setId(chamadoId);
        chamado.setTipoChamado(tipoChamado);
        chamado.setDataAbertura(LocalDateTime.now().minusHours(3));
        chamado.setDataFinalizacao(LocalDateTime.now().minusMinutes(10));

        when(chamadoRepository.findByIdAndMoradorId(chamadoId, moradorId)).thenReturn(Optional.of(chamado));
        when(statusChamadoRepository.findByNome("Atrasado")).thenReturn(Optional.of(statusAtrasado));
        when(chamadoRepository.save(any(Chamado.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Chamado reaberto = chamadoService.reabrirComoMorador(morador, chamadoId);

        assertEquals(statusAtrasado, reaberto.getStatus());
        assertNull(reaberto.getDataFinalizacao());
    }

    @Test
    void reabrirComoMoradorDeveFalharQuandoChamadoNaoEstiverFinalizado() {
        UUID moradorId = UUID.randomUUID();
        UUID chamadoId = UUID.randomUUID();
        AuthenticatedUser morador = new AuthenticatedUser(moradorId, "morador@cond.local", "ROLE_MORADOR");

        Chamado chamado = new Chamado();
        chamado.setId(chamadoId);
        chamado.setDataFinalizacao(null);

        when(chamadoRepository.findByIdAndMoradorId(chamadoId, moradorId)).thenReturn(Optional.of(chamado));

        assertThrows(
                BusinessRuleException.class,
                () -> chamadoService.reabrirComoMorador(morador, chamadoId)
        );
    }
}
