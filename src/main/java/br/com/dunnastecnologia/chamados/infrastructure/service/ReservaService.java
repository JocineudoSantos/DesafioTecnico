package br.com.dunnastecnologia.chamados.infrastructure.service;

import br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser;
import br.com.dunnastecnologia.chamados.application.UserCase.ReservaUseCases;
import br.com.dunnastecnologia.chamados.domain.model.AreaComum;
import br.com.dunnastecnologia.chamados.domain.model.Morador;
import br.com.dunnastecnologia.chamados.domain.model.Reserva;
import br.com.dunnastecnologia.chamados.domain.model.ReservaStatus;
import br.com.dunnastecnologia.chamados.infrastructure.exception.BusinessRuleException;
import br.com.dunnastecnologia.chamados.infrastructure.exception.ResourceNotFoundException;
import br.com.dunnastecnologia.chamados.infrastructure.repository.AreaComumRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.MoradorRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.ReservaRepository;
import br.com.dunnastecnologia.chamados.infrastructure.service.support.AuthenticatedUserValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReservaService implements ReservaUseCases {

    private final ReservaRepository reservaRepository;
    private final AreaComumRepository areaRepository;
    private final MoradorRepository moradorRepository;
    private final AuthenticatedUserValidator userValidator;
    private final Clock clock;

    public ReservaService(
            ReservaRepository reservaRepository,
            AreaComumRepository areaRepository,
            MoradorRepository moradorRepository,
            AuthenticatedUserValidator userValidator,
            Clock clock
    ) {
        this.reservaRepository = reservaRepository;
        this.areaRepository = areaRepository;
        this.moradorRepository = moradorRepository;
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
