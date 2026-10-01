package br.com.dunnastecnologia.chamados.infrastructure.service;

import br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser;
import br.com.dunnastecnologia.chamados.application.UserCase.AreaComumUseCases;
import br.com.dunnastecnologia.chamados.domain.model.AreaComum;
import br.com.dunnastecnologia.chamados.infrastructure.exception.BusinessRuleException;
import br.com.dunnastecnologia.chamados.infrastructure.exception.ResourceNotFoundException;
import br.com.dunnastecnologia.chamados.infrastructure.repository.AreaComumRepository;
import br.com.dunnastecnologia.chamados.infrastructure.service.support.AuthenticatedUserValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AreaComumService implements AreaComumUseCases {

    private static final int NOME_MAX = 120;
    private static final int DESCRICAO_MAX = 500;

    private final AreaComumRepository repository;
    private final AuthenticatedUserValidator userValidator;

    public AreaComumService(AreaComumRepository repository, AuthenticatedUserValidator userValidator) {
        this.repository = repository;
        this.userValidator = userValidator;
    }

    @Override
    public List<AreaComum> listarParaAdministracao(AuthenticatedUser administrador) {
        userValidator.assertAdministrador(administrador);
        return repository.findAllByOrderByNomeAsc();
    }

    @Override
    @Transactional
    public AreaComum cadastrar(AuthenticatedUser administrador, String nome, String descricao) {
        userValidator.assertAdministrador(administrador);
        AreaComum area = new AreaComum();
        area.setNome(validarNome(nome));
        area.setDescricao(validarDescricao(descricao));
        area.setAtiva(true);
        return repository.save(area);
    }

    @Override
    @Transactional
    public AreaComum atualizar(AuthenticatedUser administrador, UUID id, String nome, String descricao) {
        userValidator.assertAdministrador(administrador);
        AreaComum area = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Área comum não encontrada"));
        String nomeValidado = validarNome(nome);
        String descricaoValidada = validarDescricao(descricao);
        area.setNome(nomeValidado);
        area.setDescricao(descricaoValidada);
        return repository.save(area);
    }

    @Override
    @Transactional
    public void desativar(AuthenticatedUser administrador, UUID id) {
        userValidator.assertAdministrador(administrador);
        AreaComum area = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Área comum não encontrada"));
        area.setAtiva(false);
    }

    @Override
    public List<AreaComum> listarAtivas(AuthenticatedUser morador) {
        userValidator.assertMorador(morador);
        return repository.findByAtivaTrueOrderByNomeAsc();
    }

    private String validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new BusinessRuleException("Informe o nome da área comum.");
        }
        String valor = nome.trim();
        if (valor.length() > NOME_MAX) {
            throw new BusinessRuleException("O nome da área comum deve ter até 120 caracteres.");
        }
        return valor;
    }

    private String validarDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            return null;
        }
        String valor = descricao.trim();
        if (valor.length() > DESCRICAO_MAX) {
            throw new BusinessRuleException("A descrição deve ter até 500 caracteres.");
        }
        return valor;
    }
}
