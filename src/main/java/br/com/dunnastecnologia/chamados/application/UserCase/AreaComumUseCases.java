package br.com.dunnastecnologia.chamados.application.UserCase;

import br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser;
import br.com.dunnastecnologia.chamados.domain.model.AreaComum;

import java.util.List;
import java.util.UUID;

public interface AreaComumUseCases {

    /**
     * Existe para permitir ao administrador consultar areas ativas e inativas
     * durante a manutencao do cadastro.
     */
    List<AreaComum> listarParaAdministracao(AuthenticatedUser administrador);

    /**
     * Existe para cadastrar uma area comum que possa receber solicitacoes
     * de reserva dos moradores.
     */
    AreaComum cadastrar(
            AuthenticatedUser administrador,
            String nome,
            String descricao
    );

    /**
     * Existe para atualizar os dados descritivos de uma area comum existente.
     */
    AreaComum atualizar(
            AuthenticatedUser administrador,
            UUID id,
            String nome,
            String descricao
    );

    /**
     * Existe para impedir novas solicitacoes em uma area sem remover seu cadastro
     * nem as reservas ja registradas.
     */
    void desativar(
            AuthenticatedUser administrador,
            UUID id
    );

    /**
     * Existe para apresentar ao morador somente areas que ainda aceitam
     * solicitacoes de reserva.
     */
    List<AreaComum> listarAtivas(AuthenticatedUser morador);
}
