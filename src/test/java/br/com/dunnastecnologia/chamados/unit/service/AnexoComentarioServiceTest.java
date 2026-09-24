package br.com.dunnastecnologia.chamados.unit.service;

import br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser;
import br.com.dunnastecnologia.chamados.application.UserCase.AnexoComentarioUseCases.AnexoComentarioInfo;
import br.com.dunnastecnologia.chamados.domain.model.AnexoComentario;
import br.com.dunnastecnologia.chamados.domain.model.Chamado;
import br.com.dunnastecnologia.chamados.domain.model.Comentario;
import br.com.dunnastecnologia.chamados.domain.validation.ValidationLimits;
import br.com.dunnastecnologia.chamados.infrastructure.exception.BusinessRuleException;
import br.com.dunnastecnologia.chamados.infrastructure.repository.AnexoComentarioRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.ComentarioRepository;
import br.com.dunnastecnologia.chamados.infrastructure.service.support.ChamadoAccessSupport;
import br.com.dunnastecnologia.chamados.infrastructure.service.AnexoComentarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnexoComentarioServiceTest {

    @Mock
    private AnexoComentarioRepository anexoComentarioRepository;
    @Mock
    private ComentarioRepository comentarioRepository;
    @Mock
    private ChamadoAccessSupport chamadoAccessSupport;

    @InjectMocks
    private AnexoComentarioService anexoComentarioService;

    @Test
    void adicionarAnexoAoComentarioDevePersistirArquivoNoComentario() {
        AuthenticatedUser colaborador = new AuthenticatedUser(UUID.randomUUID(), "colaborador@cond.local", "ROLE_COLABORADOR");
        UUID chamadoId = UUID.randomUUID();
        UUID comentarioId = UUID.randomUUID();
        byte[] conteudo = "arquivo-comentario".getBytes();

        Comentario comentario = new Comentario();
        comentario.setId(comentarioId);
        comentario.setChamado(new Chamado());

        AnexoComentario salvo = new AnexoComentario();
        salvo.setId(UUID.randomUUID());
        salvo.setComentario(comentario);
        salvo.setNomeArquivo("foto.png");
        salvo.setContentType("image/png");
        salvo.setTamanhoBytes((long) conteudo.length);
        salvo.setConteudo(conteudo);

        when(chamadoAccessSupport.findAccessibleChamadoEmAberto(colaborador, chamadoId)).thenReturn(new Chamado());
        when(comentarioRepository.findByIdAndChamadoId(comentarioId, chamadoId)).thenReturn(Optional.of(comentario));
        when(anexoComentarioRepository.save(any(AnexoComentario.class))).thenReturn(salvo);

        AnexoComentarioInfo info = anexoComentarioService.adicionarAnexoAoComentario(
                colaborador,
                chamadoId,
                comentarioId,
                "foto.png",
                "image/png",
                conteudo.length,
                conteudo
        );

        assertEquals("foto.png", info.nomeArquivo());
        assertEquals("image/png", info.contentType());
        verify(chamadoAccessSupport).findAccessibleChamadoEmAberto(colaborador, chamadoId);

        ArgumentCaptor<AnexoComentario> captor = ArgumentCaptor.forClass(AnexoComentario.class);
        verify(anexoComentarioRepository).save(captor.capture());
        assertArrayEquals(conteudo, captor.getValue().getConteudo());
    }

    @Test
    void adicionarAnexoAoComentarioDeveFalharQuandoConteudoForVazio() {
        AuthenticatedUser administrador = new AuthenticatedUser(UUID.randomUUID(), "admin@cond.local", "ROLE_ADMINISTRADOR");

        assertThrows(
                BusinessRuleException.class,
                () -> anexoComentarioService.adicionarAnexoAoComentario(
                        administrador,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "arquivo.txt",
                        "text/plain",
                        0,
                        new byte[0]
                )
        );
    }

    @Test
    void adicionarAnexoAoComentarioDeveFalharQuandoChamadoEstiverFinalizado() {
        AuthenticatedUser administrador = new AuthenticatedUser(UUID.randomUUID(), "admin@cond.local", "ROLE_ADMINISTRADOR");
        UUID chamadoId = UUID.randomUUID();

        when(chamadoAccessSupport.findAccessibleChamadoEmAberto(administrador, chamadoId))
                .thenThrow(new BusinessRuleException("Chamados finalizados nao podem ser alterados"));

        assertThrows(
                BusinessRuleException.class,
                () -> anexoComentarioService.adicionarAnexoAoComentario(
                        administrador,
                        chamadoId,
                        UUID.randomUUID(),
                        "arquivo.txt",
                        "text/plain",
                        7,
                        "arquivo".getBytes()
                )
        );
    }

    @Test
    void adicionarAnexoAoComentarioDeveFalharQuandoArquivoExcederLimiteDeTamanho() {
        AuthenticatedUser administrador = new AuthenticatedUser(UUID.randomUUID(), "admin@cond.local", "ROLE_ADMINISTRADOR");
        byte[] conteudo = new byte[(int) ValidationLimits.ANEXO_TAMANHO_MAX_BYTES + 1];

        assertThrows(
                BusinessRuleException.class,
                () -> anexoComentarioService.adicionarAnexoAoComentario(
                        administrador,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "arquivo.txt",
                        "text/plain",
                        conteudo.length,
                        conteudo
                )
        );
    }
}
