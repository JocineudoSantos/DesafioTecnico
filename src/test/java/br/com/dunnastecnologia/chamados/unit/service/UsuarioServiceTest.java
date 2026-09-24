package br.com.dunnastecnologia.chamados.unit.service;

import br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser;
import br.com.dunnastecnologia.chamados.domain.model.Colaborador;
import br.com.dunnastecnologia.chamados.domain.model.Morador;
import br.com.dunnastecnologia.chamados.domain.model.TipoChamado;
import br.com.dunnastecnologia.chamados.domain.model.Unidade;
import br.com.dunnastecnologia.chamados.domain.model.Usuario;
import br.com.dunnastecnologia.chamados.infrastructure.repository.ColaboradorRepository;
import br.com.dunnastecnologia.chamados.infrastructure.exception.BusinessRuleException;
import br.com.dunnastecnologia.chamados.infrastructure.exception.ResourceNotFoundException;
import br.com.dunnastecnologia.chamados.infrastructure.repository.MoradorRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.TipoChamadoRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.UnidadeRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.UsuarioRepository;
import br.com.dunnastecnologia.chamados.infrastructure.service.support.AuthenticatedUserValidator;
import br.com.dunnastecnologia.chamados.infrastructure.service.UsuarioService;
import br.com.dunnastecnologia.chamados.domain.validation.ValidationLimits;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ColaboradorRepository colaboradorRepository;
    @Mock
    private MoradorRepository moradorRepository;
    @Mock
    private UnidadeRepository unidadeRepository;
    @Mock
    private TipoChamadoRepository tipoChamadoRepository;
    @Mock
    private AuthenticatedUserValidator authenticatedUserValidator;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void cadastrarUsuarioDeveCodificarSenhaAntesDeSalvar() {
        AuthenticatedUser admin = new AuthenticatedUser(UUID.randomUUID(), "admin@cond.local", "ROLE_ADMINISTRADOR");
        Morador usuario = new Morador();
        usuario.setNome("Maria");
        usuario.setEmail("maria@cond.local");
        usuario.setSenha("senha123");

        when(passwordEncoder.encode("senha123")).thenReturn("senha-codificada");
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        Usuario salvo = usuarioService.cadastrarUsuario(admin, usuario);

        assertEquals("senha-codificada", salvo.getSenha());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void cadastrarUsuarioDeveFalharQuandoNomeExcederLimite() {
        AuthenticatedUser admin = new AuthenticatedUser(UUID.randomUUID(), "admin@cond.local", "ROLE_ADMINISTRADOR");
        Morador usuario = new Morador();
        usuario.setNome("a".repeat(ValidationLimits.USUARIO_NOME_MAX_LENGTH + 1));
        usuario.setEmail("maria@cond.local");
        usuario.setSenha("senha123");

        assertThrows(BusinessRuleException.class, () -> usuarioService.cadastrarUsuario(admin, usuario));
    }

    @Test
    void vincularMoradorUnidadeDeveAdicionarUnidadeAoMorador() {
        AuthenticatedUser admin = new AuthenticatedUser(UUID.randomUUID(), "admin@cond.local", "ROLE_ADMINISTRADOR");
        UUID moradorId = UUID.randomUUID();
        UUID unidadeId = UUID.randomUUID();

        Morador morador = new Morador();
        morador.setId(moradorId);

        Unidade unidade = new Unidade();
        unidade.setId(unidadeId);

        when(moradorRepository.findByIdAndAtivoTrue(moradorId)).thenReturn(Optional.of(morador));
        when(unidadeRepository.findById(unidadeId)).thenReturn(Optional.of(unidade));

        usuarioService.vincularMoradorUnidade(admin, moradorId, unidadeId);

        assertEquals(1, morador.getUnidades().size());
        verify(moradorRepository).save(morador);
    }

    @Test
    void atualizarUsuarioDeveFalharQuandoTipoForAlterado() {
        AuthenticatedUser admin = new AuthenticatedUser(UUID.randomUUID(), "admin@cond.local", "ROLE_ADMINISTRADOR");
        UUID usuarioId = UUID.randomUUID();

        Usuario existente = new Morador();
        existente.setId(usuarioId);
        existente.setNome("Maria");
        existente.setEmail("maria@cond.local");
        existente.setSenha("antiga");

        Usuario novoTipo = new Usuario() {
            @Override
            public String getRole() {
                return "ROLE_ADMINISTRADOR";
            }
        };
        novoTipo.setNome("Maria");
        novoTipo.setEmail("maria@cond.local");
        novoTipo.setSenha("nova");

        when(usuarioRepository.findByIdAndAtivoTrue(usuarioId)).thenReturn(Optional.of(existente));
        when(usuarioRepository.findByEmail("maria@cond.local")).thenReturn(Optional.of(existente));

        assertThrows(BusinessRuleException.class, () -> usuarioService.atualizarUsuario(admin, usuarioId, novoTipo));
    }

    @Test
    void removerUsuarioDeveDesativarUsuarioSemExcluirRegistro() {
        AuthenticatedUser admin = new AuthenticatedUser(UUID.randomUUID(), "admin@cond.local", "ROLE_ADMINISTRADOR");
        UUID usuarioId = UUID.randomUUID();
        Morador usuario = new Morador();
        usuario.setId(usuarioId);
        usuario.setAtivo(Boolean.TRUE);

        when(usuarioRepository.findByIdAndAtivoTrue(usuarioId)).thenReturn(Optional.of(usuario));

        usuarioService.removerUsuario(admin, usuarioId);

        assertFalse(usuario.getAtivo());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void buscarUsuarioPorIdDeveIgnorarUsuarioInativo() {
        UUID usuarioId = UUID.randomUUID();
        when(usuarioRepository.findByIdAndAtivoTrue(usuarioId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> usuarioService.buscarUsuarioPorId(usuarioId));
    }

    @Test
    void listarUsuariosDeveBuscarSomenteUsuariosAtivos() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Usuario> page = new PageImpl<>(java.util.List.of(new Morador()));
        when(usuarioRepository.findAllActive(pageRequest)).thenReturn(page);

        usuarioService.listarUsuarios(pageRequest);

        verify(usuarioRepository).findAllActive(pageRequest);
    }

    @Test
    void vincularColaboradorTipoChamadoDeveAdicionarTipoAoColaborador() {
        AuthenticatedUser admin = new AuthenticatedUser(UUID.randomUUID(), "admin@cond.local", "ROLE_ADMINISTRADOR");
        UUID colaboradorId = UUID.randomUUID();
        UUID tipoChamadoId = UUID.randomUUID();

        Colaborador colaborador = new Colaborador();
        colaborador.setId(colaboradorId);

        TipoChamado tipoChamado = new TipoChamado();
        tipoChamado.setId(tipoChamadoId);

        when(colaboradorRepository.findByIdAndAtivoTrue(colaboradorId)).thenReturn(Optional.of(colaborador));
        when(tipoChamadoRepository.findById(tipoChamadoId)).thenReturn(Optional.of(tipoChamado));

        usuarioService.vincularColaboradorTipoChamado(admin, colaboradorId, tipoChamadoId);

        assertEquals(1, colaborador.getTiposChamadoResponsaveis().size());
        verify(colaboradorRepository).save(colaborador);
    }

    @Test
    void desvincularColaboradorTipoChamadoDeveRemoverTipoDoColaborador() {
        AuthenticatedUser admin = new AuthenticatedUser(UUID.randomUUID(), "admin@cond.local", "ROLE_ADMINISTRADOR");
        UUID colaboradorId = UUID.randomUUID();
        UUID tipoChamadoId = UUID.randomUUID();

        Colaborador colaborador = new Colaborador();
        colaborador.setId(colaboradorId);
        TipoChamado tipoChamado = new TipoChamado();
        tipoChamado.setId(tipoChamadoId);
        colaborador.getTiposChamadoResponsaveis().add(tipoChamado);

        when(colaboradorRepository.findByIdAndAtivoTrue(colaboradorId)).thenReturn(Optional.of(colaborador));

        usuarioService.desvincularColaboradorTipoChamado(admin, colaboradorId, tipoChamadoId);

        assertEquals(0, colaborador.getTiposChamadoResponsaveis().size());
        verify(colaboradorRepository).save(colaborador);
    }
}
