package br.com.dunnastecnologia.chamados.infrastructure.controller.web;

import br.com.dunnastecnologia.chamados.application.UserCase.AreaComumUseCases;
import br.com.dunnastecnologia.chamados.infrastructure.controller.web.form.AreaComumForm;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import br.com.dunnastecnologia.chamados.infrastructure.exception.ResourceNotFoundException;

import java.util.UUID;

@Controller
@RequestMapping("/admin/areas-comuns")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminAreaComumWebController {

    private final AreaComumUseCases areaComumUseCases;
    private final WebControllerSupport support;

    public AdminAreaComumWebController(AreaComumUseCases areaComumUseCases, WebControllerSupport support) {
        this.areaComumUseCases = areaComumUseCases;
        this.support = support;
    }

    @ModelAttribute("areaComumForm")
    public AreaComumForm areaComumForm() { return new AreaComumForm(); }

    @GetMapping
    public String listar(Authentication authentication, @RequestParam(required = false) UUID areaId, Model model) {
        var areas = areaComumUseCases.listarParaAdministracao(support.authenticatedUser(authentication));
        model.addAttribute("pageTitle", "&Aacute;reas comuns");
        model.addAttribute("areasComuns", areas);
        if (areaId != null) {
            var area = areas.stream().filter(item -> areaId.equals(item.getId())).findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Área comum não encontrada"));
            AreaComumForm form = new AreaComumForm();
            form.setNome(area.getNome());
            form.setDescricao(area.getDescricao());
            model.addAttribute("areaComumForm", form);
            model.addAttribute("areaComumEdicao", area);
        }
        return "admin/areas-comuns/lista";
    }

    @PostMapping
    public String cadastrar(Authentication authentication, @ModelAttribute AreaComumForm form, RedirectAttributes redirect) {
        areaComumUseCases.cadastrar(support.authenticatedUser(authentication), form.getNome(), form.getDescricao());
        redirect.addFlashAttribute("successMessage", "Área comum cadastrada.");
        return "redirect:/admin/areas-comuns";
    }

    @PostMapping("/{areaId}")
    public String atualizar(
            Authentication authentication,
            @PathVariable UUID areaId,
            @ModelAttribute AreaComumForm form,
            RedirectAttributes redirect
    ) {
        areaComumUseCases.atualizar(
                support.authenticatedUser(authentication), areaId, form.getNome(), form.getDescricao()
        );
        redirect.addFlashAttribute("successMessage", "Área comum atualizada.");
        return "redirect:/admin/areas-comuns";
    }

    @PostMapping("/{areaId}/desativar")
    public String desativar(Authentication authentication, @PathVariable UUID areaId, RedirectAttributes redirect) {
        areaComumUseCases.desativar(support.authenticatedUser(authentication), areaId);
        redirect.addFlashAttribute("successMessage", "Área comum desativada. As reservas existentes foram preservadas.");
        return "redirect:/admin/areas-comuns";
    }
}
