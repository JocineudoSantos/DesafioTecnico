package br.com.dunnastecnologia.chamados.unit.config;

import br.com.dunnastecnologia.chamados.domain.model.Administrador;
import br.com.dunnastecnologia.chamados.domain.model.StatusChamado;
import br.com.dunnastecnologia.chamados.infrastructure.repository.StatusChamadoRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.UsuarioRepository;
import br.com.dunnastecnologia.chamados.infrastructure.config.AdminBootstrapConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapConfigTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private StatusChamadoRepository statusChamadoRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void createDefaultAdministratorDeveGarantirStatusesPadrao() throws Exception {
        AdminBootstrapConfig config = new AdminBootstrapConfig();

        StatusChamado solicitado = new StatusChamado();
        solicitado.setId(UUID.randomUUID());
        solicitado.setNome("Solicitado");
        solicitado.setInicialPadrao(Boolean.FALSE);

        when(statusChamadoRepository.findByNome("Solicitado"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(solicitado));
        when(statusChamadoRepository.findByNome("Finalizado")).thenReturn(Optional.empty());
        when(statusChamadoRepository.findByNome("Atrasado")).thenReturn(Optional.empty());
        when(statusChamadoRepository.findByInicialPadraoTrue()).thenReturn(Optional.empty());
        when(statusChamadoRepository.save(any(StatusChamado.class))).thenAnswer(invocation -> {
            StatusChamado status = invocation.getArgument(0);
            if ("Solicitado".equals(status.getNome()) && status.getId() == null) {
                status.setId(solicitado.getId());
            }
            return status;
        });
        when(usuarioRepository.existsAdministrador()).thenReturn(true);

        CommandLineRunner runner = config.createDefaultAdministrator(
                usuarioRepository,
                statusChamadoRepository,
                passwordEncoder,
                true,
                "Administrador",
                "admin@cond.local",
                "admin123"
        );

        runner.run();

        ArgumentCaptor<StatusChamado> captor = ArgumentCaptor.forClass(StatusChamado.class);
        verify(statusChamadoRepository, atLeastOnce()).save(captor.capture());

        boolean possuiSolicitadoPadrao = captor.getAllValues().stream()
                .anyMatch(status -> "Solicitado".equals(status.getNome()) && Boolean.TRUE.equals(status.getInicialPadrao()));
        boolean possuiFinalizado = captor.getAllValues().stream()
                .anyMatch(status -> "Finalizado".equals(status.getNome()));
        boolean possuiAtrasado = captor.getAllValues().stream()
                .anyMatch(status -> "Atrasado".equals(status.getNome()));

        assertTrue(possuiSolicitadoPadrao);
        assertTrue(possuiFinalizado);
        assertTrue(possuiAtrasado);
    }

    @Test
    void createDefaultAdministratorDeveReativarAdministradorInativoComEmailPadrao() throws Exception {
        AdminBootstrapConfig config = new AdminBootstrapConfig();

        Administrador administrador = new Administrador();
        administrador.setNome("Antigo");
        administrador.setEmail("admin@cond.local");
        administrador.setAtivo(Boolean.FALSE);

        when(statusChamadoRepository.findByNome("Solicitado")).thenReturn(Optional.of(new StatusChamado()));
        when(statusChamadoRepository.findByNome("Finalizado")).thenReturn(Optional.of(new StatusChamado()));
        when(statusChamadoRepository.findByNome("Atrasado")).thenReturn(Optional.of(new StatusChamado()));
        when(statusChamadoRepository.findByInicialPadraoTrue()).thenReturn(Optional.empty());
        when(usuarioRepository.existsAdministrador()).thenReturn(false);
        when(usuarioRepository.findByEmail("admin@cond.local")).thenReturn(Optional.of(administrador));
        when(passwordEncoder.encode("admin123")).thenReturn("senha-codificada");

        CommandLineRunner runner = config.createDefaultAdministrator(
                usuarioRepository,
                statusChamadoRepository,
                passwordEncoder,
                true,
                "Administrador",
                "admin@cond.local",
                "admin123"
        );

        runner.run();

        assertTrue(administrador.getAtivo());
        verify(usuarioRepository).save(eq(administrador));
    }
}
