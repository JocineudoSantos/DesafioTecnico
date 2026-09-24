package br.com.dunnastecnologia.chamados.unit.service;

import br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser;
import br.com.dunnastecnologia.chamados.domain.model.Administrador;
import br.com.dunnastecnologia.chamados.domain.model.Chamado;
import br.com.dunnastecnologia.chamados.domain.model.Comentario;
import br.com.dunnastecnologia.chamados.domain.validation.ValidationLimits;
import br.com.dunnastecnologia.chamados.infrastructure.exception.BusinessRuleException;
import br.com.dunnastecnologia.chamados.infrastructure.repository.ComentarioRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.UsuarioRepository;
import br.com.dunnastecnologia.chamados.infrastructure.service.support.ChamadoAccessSupport;
import br.com.dunnastecnologia.chamados.infrastructure.service.ComentarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComentarioServiceTest {

    @Mock
    private ComentarioRepository comentarioRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ChamadoAccessSupport chamadoAccessSupport;

    @InjectMocks
    private ComentarioService comentarioService;

    @Test
    void comentarDevePersistirComentarioQuandoChamadoEstiverAberto() {
        AuthenticatedUser usuario = new AuthenticatedUser(UUID.randomUUID(), "admin@cond.local", "ROLE_ADMINISTRADOR");
        UUID chamadoId = UUID.randomUUID();

        Administrador autor = new Administrador();
        autor.setId(usuario.id());

        Chamado chamado = new Chamado();
        chamado.setId(chamadoId);

        when(usuarioRepository.findByIdAndAtivoTrue(usuario.id())).thenReturn(Optional.of(autor));
        when(chamadoAccessSupport.findAccessibleChamadoEmAberto(usuario, chamadoId)).thenReturn(chamado);
        when(comentarioRepository.save(any(Comentario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Comentario comentario = comentarioService.comentar(usuario, chamadoId, "Atualizacao");

        assertEquals("Atualizacao", comentario.getMensagem());
        assertEquals(autor, comentario.getAutor());
        assertEquals(chamado, comentario.getChamado());
        assertNotNull(comentario.getDataCriacao());
    }

    @Test
    void comentarDeveFalharQuandoChamadoEstiverFinalizado() {
        AuthenticatedUser usuario = new AuthenticatedUser(UUID.randomUUID(), "admin@cond.local", "ROLE_ADMINISTRADOR");
        UUID chamadoId = UUID.randomUUID();

        Administrador autor = new Administrador();
        autor.setId(usuario.id());

        when(usuarioRepository.findByIdAndAtivoTrue(usuario.id())).thenReturn(Optional.of(autor));
        when(chamadoAccessSupport.findAccessibleChamadoEmAberto(usuario, chamadoId))
                .thenThrow(new BusinessRuleException("Chamados finalizados nao podem ser alterados"));

        assertThrows(
                BusinessRuleException.class,
                () -> comentarioService.comentar(usuario, chamadoId, "Atualizacao")
        );
    }

    @Test
    void comentarDeveFalharQuandoMensagemExcederLimite() {
        AuthenticatedUser usuario = new AuthenticatedUser(UUID.randomUUID(), "admin@cond.local", "ROLE_ADMINISTRADOR");

        assertThrows(
                BusinessRuleException.class,
                () -> comentarioService.comentar(
                        usuario,
                        UUID.randomUUID(),
                        "a".repeat(ValidationLimits.COMENTARIO_MENSAGEM_MAX_LENGTH + 1)
                )
        );
    }
}
