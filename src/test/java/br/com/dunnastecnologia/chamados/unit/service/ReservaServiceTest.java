package br.com.dunnastecnologia.chamados.unit.service;

import br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser;
import br.com.dunnastecnologia.chamados.domain.model.AreaComum;
import br.com.dunnastecnologia.chamados.domain.model.Morador;
import br.com.dunnastecnologia.chamados.domain.model.ReservaStatus;
import br.com.dunnastecnologia.chamados.infrastructure.exception.BusinessRuleException;
import br.com.dunnastecnologia.chamados.infrastructure.exception.ResourceNotFoundException;
import br.com.dunnastecnologia.chamados.infrastructure.repository.AreaComumRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.MoradorRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.ReservaRepository;
import br.com.dunnastecnologia.chamados.infrastructure.service.ReservaService;
import br.com.dunnastecnologia.chamados.infrastructure.service.support.AuthenticatedUserValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReservaServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-30T15:00:00Z");
    private final ReservaRepository reservaRepository = mock(ReservaRepository.class);
    private final AreaComumRepository areaRepository = mock(AreaComumRepository.class);
    private final MoradorRepository moradorRepository = mock(MoradorRepository.class);
    private final AuthenticatedUserValidator validator = mock(AuthenticatedUserValidator.class);
    private final Clock clock = Clock.fixed(NOW, ZoneId.of("America/Sao_Paulo"));
    private final ReservaService service = new ReservaService(reservaRepository, areaRepository, moradorRepository, validator, clock);
    private final UUID areaId = UUID.randomUUID();
    private final UUID residentId = UUID.randomUUID();
    private final AuthenticatedUser resident = new AuthenticatedUser(residentId, "resident@test", "ROLE_MORADOR");
    private final AreaComum area = new AreaComum();
    private final Morador morador = new Morador();

    @BeforeEach
    void prepare() {
        area.setId(areaId);
        area.setAtiva(true);
        morador.setId(residentId);
        when(areaRepository.findByIdAndAtivaTrue(areaId)).thenReturn(Optional.of(area));
        when(moradorRepository.findByIdAndAtivoTrue(residentId)).thenReturn(Optional.of(morador));
        when(reservaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void solicitaReservaPendendoComMoradorDoTokenEHorarioDoClock() {
        var reserva = service.solicitar(resident, areaId, NOW.plusSeconds(3600), NOW.plusSeconds(7200));
        assertSame(morador, reserva.getMorador());
        assertSame(area, reserva.getAreaComum());
        assertEquals(ReservaStatus.SOLICITADA, reserva.getStatus());
        assertEquals(NOW, reserva.getCriadaEm());
        verify(reservaRepository).existeSobreposicao(areaId, ReservaStatus.APROVADA, NOW.plusSeconds(3600), NOW.plusSeconds(7200));
    }

    @Test
    void permiteSolicitacoesSobrepostasMasRecusaConflitoAprovado() {
        when(reservaRepository.existeSobreposicao(any(), any(), any(), any())).thenReturn(true);
        assertThrows(BusinessRuleException.class,
                () -> service.solicitar(resident, areaId, NOW.plusSeconds(3600), NOW.plusSeconds(7200)));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void rejeitaInicioPassadoFimInvalidoEAreaInativa() {
        assertThrows(BusinessRuleException.class, () -> service.solicitar(resident, areaId, NOW, NOW.plusSeconds(1)));
        assertThrows(BusinessRuleException.class, () -> service.solicitar(resident, areaId, NOW.plusSeconds(2), NOW.plusSeconds(1)));
        when(areaRepository.findByIdAndAtivaTrue(areaId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.solicitar(resident, areaId, NOW.plusSeconds(10), NOW.plusSeconds(20)));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void consultaSomenteConflitosAprovadosEListaDoMorador() {
        when(reservaRepository.existeSobreposicao(areaId, ReservaStatus.APROVADA, NOW.plusSeconds(1), NOW.plusSeconds(60)))
                .thenReturn(false);
        assertTrue(service.estaDisponivel(resident, areaId, NOW.plusSeconds(1), NOW.plusSeconds(60)));
        service.listarMinhasReservas(resident);
        verify(reservaRepository).listarDoMorador(residentId);
    }
}
