package br.com.dunnastecnologia.chamados.application.UserCase;

import br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser;
import br.com.dunnastecnologia.chamados.domain.model.Reserva;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ReservaUseCases {

    /**
     * Existe para que o morador acompanhe as solicitacoes e reservas
     * vinculadas a sua propria conta.
     */
    List<Reserva> listarMinhasReservas(AuthenticatedUser morador);

    /**
     * Existe para consultar se o intervalo esta livre de reservas aprovadas
     * para a area selecionada.
     */
    boolean estaDisponivel(
            AuthenticatedUser morador,
            UUID areaId,
            Instant inicio,
            Instant fim
    );

    /**
     * Existe para informar ao morador quais reservas aprovadas conflitam
     * com o periodo consultado.
     */
    List<Reserva> listarAprovadasSobrepostas(
            AuthenticatedUser morador,
            UUID areaId,
            Instant inicio,
            Instant fim
    );

    /**
     * Existe para registrar uma solicitacao de reserva em estado SOLICITADA,
     * apos validar a area e o periodo informado.
     */
    Reserva solicitar(
            AuthenticatedUser morador,
            UUID areaId,
            Instant inicio,
            Instant fim
    );

    /** Lista as reservas para a consulta e moderação administrativa. */
    List<Reserva> listarParaAdministracao(AuthenticatedUser administrador);

    /** Aprova uma solicitação somente se não houver conflito aprovado para a mesma área. */
    Reserva aprovar(AuthenticatedUser administrador, UUID reservaId);

    /** Nega uma solicitação e registra a justificativa informada. */
    Reserva negar(AuthenticatedUser administrador, UUID reservaId, String motivo);

    /** Cancela reserva solicitada/aprovada antes do início, por seu proprietário ou administrador. */
    Reserva cancelar(AuthenticatedUser ator, UUID reservaId);
}
