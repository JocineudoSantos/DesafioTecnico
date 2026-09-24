package br.com.dunnastecnologia.chamados.unit.service;

import br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser;
import br.com.dunnastecnologia.chamados.application.UserCase.AnexoChamadoUseCases.AnexoChamadoInfo;
import br.com.dunnastecnologia.chamados.domain.model.AnexoChamado;
import br.com.dunnastecnologia.chamados.domain.model.Chamado;
import br.com.dunnastecnologia.chamados.domain.validation.ValidationLimits;
import br.com.dunnastecnologia.chamados.infrastructure.exception.BusinessRuleException;
import br.com.dunnastecnologia.chamados.infrastructure.repository.AnexoChamadoRepository;
import br.com.dunnastecnologia.chamados.infrastructure.service.support.ChamadoAccessSupport;
import br.com.dunnastecnologia.chamados.infrastructure.service.AnexoChamadoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnexoChamadoServiceTest {

    @Mock
    private AnexoChamadoRepository anexoChamadoRepository;
    @Mock
    private ChamadoAccessSupport chamadoAccessSupport;

    @InjectMocks
    private AnexoChamadoService anexoChamadoService;

    @Test
    void adicionarAnexoDevePersistirArquivoNoChamado() {
        AuthenticatedUser usuario = new AuthenticatedUser(UUID.randomUUID(), "morador@cond.local", "ROLE_MORADOR");
        UUID chamadoId = UUID.randomUUID();
        byte[] conteudo = "arquivo".getBytes();

        Chamado chamado = new Chamado();
        chamado.setId(chamadoId);

        AnexoChamado salvo = new AnexoChamado();
        salvo.setId(UUID.randomUUID());
        salvo.setChamado(chamado);
        salvo.setNomeArquivo("foto.png");
        salvo.setContentType("image/png");
        salvo.setTamanhoBytes((long) conteudo.length);
        salvo.setConteudo(conteudo);

        when(chamadoAccessSupport.findAccessibleChamadoEmAberto(usuario, chamadoId)).thenReturn(chamado);
        when(anexoChamadoRepository.save(any(AnexoChamado.class))).thenReturn(salvo);

        AnexoChamadoInfo info = anexoChamadoService.adicionarAnexo(
                usuario,
                chamadoId,
                "foto.png",
                "image/png",
                conteudo.length,
                conteudo
        );

        assertEquals("foto.png", info.nomeArquivo());
        assertEquals("image/png", info.contentType());

        ArgumentCaptor<AnexoChamado> captor = ArgumentCaptor.forClass(AnexoChamado.class);
        verify(anexoChamadoRepository).save(captor.capture());
        assertArrayEquals(conteudo, captor.getValue().getConteudo());
    }

    @Test
    void adicionarAnexoDeveFalharQuandoChamadoEstiverFinalizado() {
        AuthenticatedUser usuario = new AuthenticatedUser(UUID.randomUUID(), "morador@cond.local", "ROLE_MORADOR");
        UUID chamadoId = UUID.randomUUID();

        when(chamadoAccessSupport.findAccessibleChamadoEmAberto(usuario, chamadoId))
                .thenThrow(new BusinessRuleException("Chamados finalizados nao podem ser alterados"));

        assertThrows(
                BusinessRuleException.class,
                () -> anexoChamadoService.adicionarAnexo(
                        usuario,
                        chamadoId,
                        "arquivo.txt",
                        "text/plain",
                        7,
                        "arquivo".getBytes()
                )
        );
    }

    @Test
    void adicionarAnexoDeveFalharQuandoArquivoExcederLimiteDeTamanho() {
        AuthenticatedUser usuario = new AuthenticatedUser(UUID.randomUUID(), "morador@cond.local", "ROLE_MORADOR");
        byte[] conteudo = new byte[(int) ValidationLimits.ANEXO_TAMANHO_MAX_BYTES + 1];

        assertThrows(
                BusinessRuleException.class,
                () -> anexoChamadoService.adicionarAnexo(
                        usuario,
                        UUID.randomUUID(),
                        "arquivo.txt",
                        "text/plain",
                        conteudo.length,
                        conteudo
                )
        );
    }

    @Test
    void adicionarAnexoDeveFalharQuandoConteudoForVazio() {
        AuthenticatedUser usuario = new AuthenticatedUser(UUID.randomUUID(), "morador@cond.local", "ROLE_MORADOR");

        assertThrows(
                BusinessRuleException.class,
                () -> anexoChamadoService.adicionarAnexo(
                        usuario,
                        UUID.randomUUID(),
                        "arquivo.txt",
                        "text/plain",
                        0,
                        new byte[0]
                )
        );
    }
}
