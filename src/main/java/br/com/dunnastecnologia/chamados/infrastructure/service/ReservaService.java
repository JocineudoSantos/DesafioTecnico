package br.com.dunnastecnologia.chamados.infrastructure.service;

import br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser;
import br.com.dunnastecnologia.chamados.application.UserCase.ReservaUseCases;
import br.com.dunnastecnologia.chamados.domain.model.AreaComum;
import br.com.dunnastecnologia.chamados.domain.model.Morador;
import br.com.dunnastecnologia.chamados.domain.model.Reserva;
import br.com.dunnastecnologia.chamados.domain.model.ReservaStatus;
import br.com.dunnastecnologia.chamados.domain.model.Usuario;
import br.com.dunnastecnologia.chamados.infrastructure.exception.BusinessRuleException;
import br.com.dunnastecnologia.chamados.infrastructure.exception.ResourceNotFoundException;
import br.com.dunnastecnologia.chamados.infrastructure.exception.UnauthorizedOperationException;
import br.com.dunnastecnologia.chamados.infrastructure.repository.AdministradorRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.AreaComumRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.MoradorRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.ReservaRepository;
import br.com.dunnastecnologia.chamados.infrastructure.service.support.AuthenticatedUserValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReservaService implements ReservaUseCases {

    private final ReservaRepository reservaRepository;
    private final AreaComumRepository areaRepository;
    private final MoradorRepository moradorRepository;
    private final AdministradorRepository administradorRepository;
    private final AuthenticatedUserValidator userValidator;
    private final Clock clock;

    public ReservaService(
            ReservaRepository reservaRepository,
            AreaComumRepository areaRepository,
            MoradorRepository moradorRepository,
            AdministradorRepository administradorRepository,
            AuthenticatedUserValidator userValidator,
            Clock clock
    ) {
        this.reservaRepository = reservaRepository;
        this.areaRepository = areaRepository;
        this.moradorRepository = moradorRepository;
        this.administradorRepository = administradorRepository;
        this.userValidator = userValidator;
        this.clock = clock;
    }

    @Override
    public List<Reserva> listarMinhasReservas(AuthenticatedUser morador) {
        userValidator.assertMorador(morador);
        return reservaRepository.listarDoMorador(morador.id());
    }

    @Override
    public boolean estaDisponivel(AuthenticatedUser morador, UUID areaId, Instant inicio, Instant fim) {
        userValidator.assertMorador(morador);
        validarPeriodo(inicio, fim);
        obterAreaAtiva(areaId);
        return !reservaRepository.existeSobreposicao(areaId, ReservaStatus.APROVADA, inicio, fim);
    }

    @Override
    public List<Reserva> listarAprovadasSobrepostas(
            AuthenticatedUser morador, UUID areaId, Instant inicio, Instant fim
    ) {
        userValidator.assertMorador(morador);
        validarPeriodo(inicio, fim);
        obterAreaAtiva(areaId);
        return reservaRepository.listarSobreposicoes(areaId, ReservaStatus.APROVADA, inicio, fim);
    }

    @Override
    @Transactional
    public Reserva solicitar(AuthenticatedUser morador, UUID areaId, Instant inicio, Instant fim) {
        userValidator.assertMorador(morador);
        validarPeriodo(inicio, fim);
        if (!inicio.isAfter(clock.instant())) {
            throw new BusinessRuleException("O início da reserva deve estar no futuro.");
        }
        AreaComum area = obterAreaAtiva(areaId);
        if (reservaRepository.existeSobreposicao(areaId, ReservaStatus.APROVADA, inicio, fim)) {
            throw new BusinessRuleException("O período conflita com uma reserva aprovada desta área.");
        }

        Morador solicitante = moradorRepository.findByIdAndAtivoTrue(morador.id())
                .orElseThrow(() -> new ResourceNotFoundException("Morador não encontrado"));
        Reserva reserva = new Reserva();
        reserva.setAreaComum(area);
        reserva.setMorador(solicitante);
        reserva.setInicio(inicio);
        reserva.setFim(fim);
        reserva.setStatus(ReservaStatus.SOLICITADA);
        reserva.setCriadaEm(clock.instant());
        return reservaRepository.save(reserva);
    }

    @Override
    public List<Reserva> listarParaAdministracao(AuthenticatedUser administrador) {
        userValidator.assertAdministrador(administrador);
        return reservaRepository.listarTodasParaAdministracao();
    }

    @Override
    @Transactional
    public Reserva aprovar(AuthenticatedUser administrador, UUID reservaId) {
        userValidator.assertAdministrador(administrador);
        Reserva reserva = obterSolicitacaoBloqueada(reservaId);
        UUID areaId = reserva.getAreaComum().getId();
        areaRepository.findByIdForUpdate(areaId)
                .orElseThrow(() -> new ResourceNotFoundException("Área comum não encontrada"));
        validarPendente(reserva);
        if (reservaRepository.existeSobreposicao(
                areaId, ReservaStatus.APROVADA, reserva.getInicio(), reserva.getFim())) {
            throw new BusinessRuleException("Não é possível aprovar: o período conflita com outra reserva aprovada.");
        }

        reserva.setStatus(ReservaStatus.APROVADA);
        registrarDecisao(reserva, administrador);
        return reservaRepository.save(reserva);
    }

    @Override
    @Transactional
    public Reserva negar(AuthenticatedUser administrador, UUID reservaId, String motivo) {
        userValidator.assertAdministrador(administrador);
        if (!StringUtils.hasText(motivo)) {
            throw new BusinessRuleException("Informe um motivo para negar a solicitação.");
        }
        Reserva reserva = obterSolicitacaoBloqueada(reservaId);
        validarPendente(reserva);
        reserva.setStatus(ReservaStatus.NEGADA);
        reserva.setMotivoNegacao(motivo.strip());
        registrarDecisao(reserva, administrador);
        return reservaRepository.save(reserva);
    }

    @Override
    @Transactional
    public Reserva cancelar(AuthenticatedUser ator, UUID reservaId) {
        boolean administrador = userValidator.isAdministrador(ator);
        boolean morador = userValidator.isMorador(ator);
        if (!administrador && !morador) {
            throw new UnauthorizedOperationException("Somente o morador proprietário ou um administrador pode cancelar a reserva.");
        }
        if (administrador) {
            userValidator.assertAdministrador(ator);
        } else {
            userValidator.assertMorador(ator);
        }

        Reserva reserva = obterReservaBloqueada(reservaId);
        if (!administrador && !Objects.equals(reserva.getMorador().getId(), ator.id())) {
            throw new UnauthorizedOperationException("O morador só pode cancelar as próprias reservas.");
        }
        if (reserva.getStatus() != ReservaStatus.SOLICITADA
                && reserva.getStatus() != ReservaStatus.APROVADA) {
            throw new BusinessRuleException("Somente reservas solicitadas ou aprovadas podem ser canceladas.");
        }
        Instant agora = clock.instant();
        if (!reserva.getInicio().isAfter(agora)) {
            throw new BusinessRuleException("Não é possível cancelar uma reserva cujo horário de início já chegou.");
        }

        Usuario cancelador = administrador
                ? administradorRepository.findByIdAndAtivoTrue(ator.id())
                    .orElseThrow(() -> new ResourceNotFoundException("Administrador não encontrado"))
                : moradorRepository.findByIdAndAtivoTrue(ator.id())
                    .orElseThrow(() -> new ResourceNotFoundException("Morador não encontrado"));
        reserva.setStatus(ReservaStatus.CANCELADA);
        reserva.setCanceladaEm(agora);
        reserva.setCanceladaPor(cancelador);
        return reservaRepository.save(reserva);
    }

    private Reserva obterReservaBloqueada(UUID reservaId) {
        if (reservaId == null) {
            throw new BusinessRuleException("Selecione uma reserva.");
        }
        return reservaRepository.findByIdForUpdate(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada"));
    }
    private Reserva obterSolicitacaoBloqueada(UUID reservaId) {
        if (reservaId == null) {
            throw new BusinessRuleException("Selecione uma solicitação.");
        }
        return reservaRepository.findByIdForUpdate(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada"));
    }

    private void validarPendente(Reserva reserva) {
        if (reserva.getStatus() != ReservaStatus.SOLICITADA) {
            throw new BusinessRuleException("Somente solicitações pendentes podem ser decididas.");
        }
    }

    private void registrarDecisao(Reserva reserva, AuthenticatedUser administrador) {
        Usuario autor = administradorRepository.findByIdAndAtivoTrue(administrador.id())
                .orElseThrow(() -> new ResourceNotFoundException("Administrador não encontrado"));
        reserva.setDecididaPor(autor);
        reserva.setDecididaEm(clock.instant());
    }

    private AreaComum obterAreaAtiva(UUID areaId) {
        if (areaId == null) {
            throw new BusinessRuleException("Selecione uma área comum.");
        }
        return areaRepository.findByIdAndAtivaTrue(areaId)
                .orElseThrow(() -> new ResourceNotFoundException("Área comum indisponível ou não encontrada"));
    }

    private void validarPeriodo(Instant inicio, Instant fim) {
        if (inicio == null || fim == null) {
            throw new BusinessRuleException("Informe o início e o fim do período.");
        }
        if (!fim.isAfter(inicio)) {
            throw new BusinessRuleException("O fim deve ser posterior ao início.");
        }
    }
}
