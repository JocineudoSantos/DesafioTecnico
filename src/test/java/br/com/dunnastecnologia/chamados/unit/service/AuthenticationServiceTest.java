package br.com.dunnastecnologia.chamados.unit.service;

import br.com.dunnastecnologia.chamados.domain.model.Morador;
import br.com.dunnastecnologia.chamados.infrastructure.repository.UsuarioRepository;
import br.com.dunnastecnologia.chamados.infrastructure.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void loadUserByUsernameDeveBuscarSomenteUsuariosAtivos() {
        Morador morador = new Morador();
        morador.setEmail("morador@cond.local");

        when(usuarioRepository.findActiveByEmail("morador@cond.local")).thenReturn(Optional.of(morador));

        authenticationService.loadUserByUsername("morador@cond.local");

        verify(usuarioRepository).findActiveByEmail("morador@cond.local");
    }

    @Test
    void loadUserByUsernameDeveFalharQuandoUsuarioEstiverInativoOuNaoExistir() {
        when(usuarioRepository.findActiveByEmail("inativo@cond.local")).thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> authenticationService.loadUserByUsername("inativo@cond.local")
        );
    }
}
