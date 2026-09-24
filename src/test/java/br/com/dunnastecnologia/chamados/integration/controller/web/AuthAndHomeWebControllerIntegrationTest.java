package br.com.dunnastecnologia.chamados.integration.controller.web;

import br.com.dunnastecnologia.chamados.infrastructure.controller.web.AuthWebController;
import br.com.dunnastecnologia.chamados.infrastructure.controller.web.HomeWebController;
import br.com.dunnastecnologia.chamados.infrastructure.controller.web.WebControllerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest({AuthWebController.class, HomeWebController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(WebControllerSupport.class)
class AuthAndHomeWebControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void homeDeveRedirecionarParaLoginQuandoNaoHaSessaoAutenticada() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void homeDeveRedirecionarAdministradorParaPainelCorreto() throws Exception {
        mockMvc.perform(get("/").with(authentication(WebTestAuthenticationFactory.administrador())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void loginDeveRenderizarTelaPublica() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }
}
