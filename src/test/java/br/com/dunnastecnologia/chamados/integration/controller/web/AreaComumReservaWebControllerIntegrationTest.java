package br.com.dunnastecnologia.chamados.integration.controller.web;

import br.com.dunnastecnologia.chamados.application.UserCase.AreaComumUseCases;
import br.com.dunnastecnologia.chamados.application.UserCase.ReservaUseCases;
import br.com.dunnastecnologia.chamados.domain.model.AreaComum;
import br.com.dunnastecnologia.chamados.domain.model.Morador;
import br.com.dunnastecnologia.chamados.domain.model.Reserva;
import br.com.dunnastecnologia.chamados.domain.model.ReservaStatus;
import br.com.dunnastecnologia.chamados.infrastructure.controller.web.AdminAreaComumWebController;
import br.com.dunnastecnologia.chamados.infrastructure.controller.web.AdminReservaWebController;
import br.com.dunnastecnologia.chamados.infrastructure.controller.web.MoradorReservaWebController;
import br.com.dunnastecnologia.chamados.infrastructure.controller.web.WebControllerSupport;
import br.com.dunnastecnologia.chamados.infrastructure.exception.BusinessRuleException;
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
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AdminAreaComumWebController.class, AdminReservaWebController.class, MoradorReservaWebController.class})
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
    void administradorAbreFilaDeReservas() throws Exception {
        when(reservaUseCases.listarParaAdministracao(any())).thenReturn(List.of());

        mockMvc.perform(get("/admin/reservas")
                        .with(authentication(WebTestAuthenticationFactory.administrador())))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reservas"))
                .andExpect(model().attributeExists("reservas", "quantidadePendentes"))
                .andExpect(model().attribute("pageTitle", "Solicita&ccedil;&otilde;es de reserva"));
    }

    @Test
    void filaAdministrativaApresentaOsDadosDaReserva() throws Exception {
        AreaComum area = new AreaComum();
        area.setNome("Salão");
        Morador morador = new Morador();
        morador.setNome("Ana Moradora");
        morador.setEmail("ana@example.test");
        Reserva reserva = new Reserva();
        reserva.setId(UUID.randomUUID());
        reserva.setAreaComum(area);
        reserva.setMorador(morador);
        reserva.setInicio(Instant.parse("2027-05-10T13:00:00Z"));
        reserva.setFim(Instant.parse("2027-05-10T14:00:00Z"));
        reserva.setCriadaEm(Instant.parse("2027-05-01T10:00:00Z"));
        reserva.setStatus(ReservaStatus.SOLICITADA);
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        when(reservaUseCases.listarParaAdministracao(any())).thenReturn(List.of(reserva));

        mockMvc.perform(get("/admin/reservas")
                        .with(authentication(WebTestAuthenticationFactory.administrador())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("quantidadePendentes", 1L))
                .andExpect(model().attribute("reservas", org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.hasEntry("area", "Salão"),
                                org.hamcrest.Matchers.hasEntry("morador", "Ana Moradora"),
                                org.hamcrest.Matchers.hasEntry("status", "SOLICITADA")
                        ))));
    }

    @Test
    void moradorNaoAcessaFilaAdministrativaDeReservas() throws Exception {
        mockMvc.perform(get("/admin/reservas")
                        .with(authentication(WebTestAuthenticationFactory.morador())))
                .andExpect(status().isForbidden());
        verifyNoInteractions(reservaUseCases);
    }

    @Test
    void administradorAprovaReservaPelaTela() throws Exception {
        var administrador = WebTestAuthenticationFactory.administrador();
        var reservaId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000020");

        mockMvc.perform(post("/admin/reservas/{id}/aprovar", reservaId)
                        .with(authentication(administrador)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/reservas"))
                .andExpect(flash().attribute("successMessage", "Reserva aprovada."));

        verify(reservaUseCases).aprovar(any(), eq(reservaId));
    }

    @Test
    void administradorNegaReservaComMotivoPelaTela() throws Exception {
        var administrador = WebTestAuthenticationFactory.administrador();
        var reservaId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000021");

        mockMvc.perform(post("/admin/reservas/{id}/negar", reservaId)
                        .with(authentication(administrador))
                        .param("motivo", "Manutenção programada"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/reservas"))
                .andExpect(flash().attribute("successMessage", "Solicitação negada."));

        verify(reservaUseCases).negar(any(), eq(reservaId), eq("Manutenção programada"));
    }

    @Test
    void motivoVazioExibeErroEVoltaParaFilaAdministrativa() throws Exception {
        var administrador = WebTestAuthenticationFactory.administrador();
        var reservaId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000022");
        doThrow(new BusinessRuleException("Informe um motivo para negar a solicitação."))
                .when(reservaUseCases).negar(any(), eq(reservaId), eq("   "));

        mockMvc.perform(post("/admin/reservas/{id}/negar", reservaId)
                        .with(authentication(administrador))
                        .header("Referer", "/admin/reservas")
                        .param("motivo", "   "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/reservas"))
                .andExpect(flash().attribute("errorMessage", "Informe um motivo para negar a solicitação."));
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

    @Test
    void calendarioAdministrativoMostraPendentesEAprovadasDoMesSelecionado() throws Exception {
        Reserva aprovada = criarReservaCalendario(ReservaStatus.APROVADA,
                "2027-05-10T13:00:00Z", "2027-05-10T14:00:00Z", "Salão", "Ana Moradora");
        Reserva solicitada = criarReservaCalendario(ReservaStatus.SOLICITADA,
                "2027-05-10T15:00:00Z", "2027-05-10T16:00:00Z", "Churrasqueira", "Bruno Morador");
        Reserva negada = criarReservaCalendario(ReservaStatus.NEGADA,
                "2027-05-10T17:00:00Z", "2027-05-10T18:00:00Z", "Sala", "Carla Moradora");
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        when(reservaUseCases.listarParaAdministracao(any())).thenReturn(List.of(aprovada, solicitada, negada));

        MvcResult resultado = mockMvc.perform(get("/admin/reservas")
                        .param("view", "calendario")
                        .param("mes", "2027-05")
                        .with(authentication(WebTestAuthenticationFactory.administrador())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("visualizacao", "calendario"))
                .andExpect(model().attribute("mesExibicao", "Maio de 2027"))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dias = (List<Map<String, Object>>) resultado.getModelAndView()
                .getModel().get("diasCalendario");
        assertEquals(42, dias.size());
        Map<String, Object> dia = dias.stream()
                .filter(item -> LocalDate.of(2027, 5, 10).equals(item.get("data")))
                .findFirst().orElseThrow();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> eventos = (List<Map<String, Object>>) dia.get("reservas");
        assertEquals(2, eventos.size());
        assertTrue(eventos.stream().anyMatch(evento -> "Ana Moradora".equals(evento.get("morador"))
                && "APROVADA".equals(evento.get("status"))));
        assertTrue(eventos.stream().anyMatch(evento -> "Bruno Morador".equals(evento.get("morador"))
                && "SOLICITADA".equals(evento.get("status"))));
    }

    @Test
    void calendarioDoMoradorUsaSomenteReservasDaContaAutenticada() throws Exception {
        Reserva propria = criarReservaCalendario(ReservaStatus.APROVADA,
                "2027-05-10T13:00:00Z", "2027-05-10T14:00:00Z", "Salão", "Morador da conta");
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        when(reservaUseCases.listarMinhasReservas(any())).thenReturn(List.of(propria));

        MvcResult resultado = mockMvc.perform(get("/morador/reservas")
                        .param("view", "calendario")
                        .param("mes", "2027-05")
                        .with(authentication(WebTestAuthenticationFactory.morador())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("visualizacao", "calendario"))
                .andExpect(model().attribute("mesExibicao", "Maio de 2027"))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dias = (List<Map<String, Object>>) resultado.getModelAndView()
                .getModel().get("diasCalendario");
        Map<String, Object> dia = dias.stream()
                .filter(item -> LocalDate.of(2027, 5, 10).equals(item.get("data")))
                .findFirst().orElseThrow();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> eventos = (List<Map<String, Object>>) dia.get("reservas");
        assertEquals(1, eventos.size());
        assertEquals("Salão", eventos.get(0).get("area"));
        assertFalse(eventos.get(0).containsKey("morador"));
        verify(reservaUseCases).listarMinhasReservas(any());
        verify(reservaUseCases, never()).listarParaAdministracao(any());
    }

    private Reserva criarReservaCalendario(
            ReservaStatus status, String inicio, String fim, String nomeArea, String nomeMorador
    ) {
        AreaComum area = new AreaComum();
        area.setNome(nomeArea);
        Morador morador = new Morador();
        morador.setNome(nomeMorador);
        Reserva reserva = new Reserva();
        reserva.setAreaComum(area);
        reserva.setMorador(morador);
        reserva.setInicio(Instant.parse(inicio));
        reserva.setFim(Instant.parse(fim));
        reserva.setStatus(status);
        reserva.setCriadaEm(Instant.parse("2027-05-01T00:00:00Z"));
        return reserva;
    }
}
