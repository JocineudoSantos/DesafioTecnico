package br.com.dunnastecnologia.chamados.unit.service;

import br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser;
import br.com.dunnastecnologia.chamados.domain.model.AreaComum;
import br.com.dunnastecnologia.chamados.infrastructure.exception.BusinessRuleException;
import br.com.dunnastecnologia.chamados.infrastructure.repository.AreaComumRepository;
import br.com.dunnastecnologia.chamados.infrastructure.service.AreaComumService;
import br.com.dunnastecnologia.chamados.infrastructure.service.support.AuthenticatedUserValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AreaComumServiceTest {
    private final AreaComumRepository repository = mock(AreaComumRepository.class);
    private final AuthenticatedUserValidator validator = mock(AuthenticatedUserValidator.class);
    private final AreaComumService service = new AreaComumService(repository, validator);
    private final AuthenticatedUser admin = new AuthenticatedUser(UUID.randomUUID(), "admin@test", "ROLE_ADMINISTRADOR");
    private final AuthenticatedUser resident = new AuthenticatedUser(UUID.randomUUID(), "resident@test", "ROLE_MORADOR");

    @BeforeEach
    void saveReturnsEntity() { when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0)); }

    @Test
    void cadastrarNormalizaDadosEAtivaArea() {
        AreaComum area = service.cadastrar(admin, " Salão ", "  Para eventos  ");
        assertEquals("Salão", area.getNome());
        assertEquals("Para eventos", area.getDescricao());
        assertTrue(area.getAtiva());
        verify(validator).assertAdministrador(admin);
    }

    @Test
    void cadastrarRecusaNomeVazioOuExcessivo() {
        assertThrows(BusinessRuleException.class, () -> service.cadastrar(admin, "  ", null));
        assertThrows(BusinessRuleException.class, () -> service.cadastrar(admin, "a".repeat(121), null));
        verify(repository, never()).save(any());
    }

    @Test
    void atualizarRecusaDescricaoExcessivaSemAlterarArea() {
        UUID id = UUID.randomUUID();
        AreaComum area = new AreaComum();
        area.setId(id);
        area.setNome("Antiga");
        area.setAtiva(false);
        when(repository.findById(id)).thenReturn(Optional.of(area));
        assertThrows(BusinessRuleException.class, () -> service.atualizar(admin, id, "Nova", "d".repeat(501)));
        assertFalse(area.getAtiva());
        assertEquals("Antiga", area.getNome());
        verify(repository, never()).save(any());
    }

    @Test
    void atualizarEDesativarPreservamRegistro() {
        UUID id = UUID.randomUUID();
        AreaComum area = new AreaComum();
        area.setId(id);
        area.setAtiva(true);
        when(repository.findById(id)).thenReturn(Optional.of(area));
        service.atualizar(admin, id, "Quadra", null);
        service.desativar(admin, id);
        assertFalse(area.getAtiva());
        verify(repository, times(2)).findById(id);
    }

    @Test
    void listasValidamPerfilNoServico() {
        when(repository.findAllByOrderByNomeAsc()).thenReturn(List.of());
        when(repository.findByAtivaTrueOrderByNomeAsc()).thenReturn(List.of());
        service.listarParaAdministracao(admin);
        service.listarAtivas(resident);
        verify(validator).assertAdministrador(admin);
        verify(validator).assertMorador(resident);
    }
}
