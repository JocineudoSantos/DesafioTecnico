package br.com.dunnastecnologia.chamados.unit.service;

import br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser;
import br.com.dunnastecnologia.chamados.domain.model.StatusChamado;
import br.com.dunnastecnologia.chamados.infrastructure.exception.BusinessRuleException;
import br.com.dunnastecnologia.chamados.infrastructure.repository.StatusChamadoRepository;
import br.com.dunnastecnologia.chamados.infrastructure.service.support.AuthenticatedUserValidator;
import br.com.dunnastecnologia.chamados.infrastructure.service.StatusChamadoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatusChamadoServiceTest {

    @Mock
    private StatusChamadoRepository statusChamadoRepository;

    @Mock
    private AuthenticatedUserValidator authenticatedUserValidator;

    @InjectMocks
    private StatusChamadoService statusChamadoService;

    @Test
    void atualizarStatusDeveBloquearEdicaoDeStatusReservado() {
        UUID statusId = UUID.randomUUID();
        AuthenticatedUser admin = new AuthenticatedUser(UUID.randomUUID(), "admin@cond.local", "ROLE_ADMINISTRADOR");

        StatusChamado status = new StatusChamado();
        status.setId(statusId);
        status.setNome("Finalizado");

        when(statusChamadoRepository.findById(statusId)).thenReturn(Optional.of(status));

        assertThrows(
                BusinessRuleException.class,
                () -> statusChamadoService.atualizarStatus(admin, statusId, "Encerrado")
        );

        verify(statusChamadoRepository, never()).save(status);
    }
}
