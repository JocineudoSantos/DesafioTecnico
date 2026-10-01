package br.com.dunnastecnologia.chamados.integration.controller.web;

import br.com.dunnastecnologia.chamados.application.UserCase.AreaComumUseCases;
import br.com.dunnastecnologia.chamados.application.UserCase.ReservaUseCases;
import br.com.dunnastecnologia.chamados.domain.model.AreaComum;
import br.com.dunnastecnologia.chamados.domain.model.Reserva;
import br.com.dunnastecnologia.chamados.infrastructure.controller.web.AdminAreaComumWebController;
import br.com.dunnastecnologia.chamados.infrastructure.controller.web.MoradorReservaWebController;
import br.com.dunnastecnologia.chamados.infrastructure.controller.web.WebControllerSupport;
import br.com.dunnastecnologia.chamados.infrastructure.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AdminAreaComumWebController.class, MoradorReservaWebController.class})
@AutoConfigureMockMvc
@Import({WebControllerSupport.class, WebTestSecurityConfig.class})
class AreaComumReservaWebControllerIntegrationTest {

    @TestConfiguration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class MethodSecurityConfig { }

    @Autowired MockMvc mockMvc;

    @MockitoBean JwtService jwtService;
    @MockitoBean AreaComumUseCases areaComumUseCases;
    @MockitoBean ReservaUseCases reservaUseCases;
    @MockitoBean Clock clock;

    @BeforeEach
    void setTimeZone() { when(clock.getZone()).thenReturn(ZoneId.of("America/Sao_Paulo")); }

    @Test
    void administradorListaAreasEExibeTela() throws Exception {
        when(areaComumUseCases.listarParaAdministracao(any())).thenReturn(List.of(new AreaComum()));
        mockMvc.perform(get("/admin/areas-comuns").with(authentication(WebTestAuthenticationFactory.administrador())))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/areas-comuns/lista"))
                .andExpect(model().attributeExists("areasComuns", "areaComumForm"))
                .andExpect(model().attribute("pageTitle", "&Aacute;reas comuns"));
    }

    @Test
    void colaboradorNaoAcessaCadastroDeAreas() throws Exception {
        mockMvc.perform(get("/admin/areas-comuns").with(authentication(WebTestAuthenticationFactory.colaborador())))
                .andExpect(status().isForbidden());
        verifyNoInteractions(areaComumUseCases);
    }

    @Test
    void moradorConsultaSomenteSuaTelaDeReservas() throws Exception {
        when(areaComumUseCases.listarAtivas(any())).thenReturn(List.of());
        when(reservaUseCases.listarMinhasReservas(any())).thenReturn(List.of());
        mockMvc.perform(get("/morador/reservas").with(authentication(WebTestAuthenticationFactory.morador())))
                .andExpect(status().isOk())
                .andExpect(view().name("morador/reservas/lista"))
                .andExpect(model().attributeExists("areasComuns", "minhasReservas"))
                .andExpect(model().attribute("pageTitle", "&Aacute;reas comuns e reservas"));
    }

    @Test
    void administradorNaoAcessaReservasDoMorador() throws Exception {
        mockMvc.perform(get("/morador/reservas").with(authentication(WebTestAuthenticationFactory.administrador())))
                .andExpect(status().isForbidden());
        verifyNoInteractions(reservaUseCases, areaComumUseCases);
    }

    @Test
    void moradorSolicitaComDataEAreaSemEnviarIdDoProprietario() throws Exception {
        mockMvc.perform(post("/morador/reservas")
                        .with(authentication(WebTestAuthenticationFactory.morador()))
                        .param("areaId", "00000000-0000-0000-0000-000000000010")
                        .param("inicio", "2027-05-10T10:00")
                        .param("fim", "2027-05-10T11:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/morador/reservas"));
        verify(reservaUseCases).solicitar(eq(new br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser(
                        java.util.UUID.fromString("00000000-0000-0000-0000-000000000003"),
                        "morador@condominio.local", "ROLE_MORADOR")),
                eq(java.util.UUID.fromString("00000000-0000-0000-0000-000000000010")), any(), any());
    }
}
