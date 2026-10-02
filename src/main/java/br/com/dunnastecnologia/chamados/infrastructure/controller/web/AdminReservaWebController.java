package br.com.dunnastecnologia.chamados.infrastructure.controller.web;

import br.com.dunnastecnologia.chamados.application.UserCase.ReservaUseCases;
import br.com.dunnastecnologia.chamados.domain.model.Reserva;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/admin/reservas")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminReservaWebController {

    private final ReservaUseCases reservaUseCases;
    private final WebControllerSupport support;
    private final Clock clock;

    public AdminReservaWebController(ReservaUseCases reservaUseCases, WebControllerSupport support, Clock clock) {
        this.reservaUseCases = reservaUseCases;
        this.support = support;
        this.clock = clock;
    }

    @GetMapping
    public String listar(Authentication authentication, Model model) {
        var administrador = support.authenticatedUser(authentication);
        var reservas = reservaUseCases.listarParaAdministracao(administrador).stream()
                .map(this::mapearReserva)
                .toList();
        model.addAttribute("pageTitle", "Solicita&ccedil;&otilde;es de reserva");
        model.addAttribute("reservas", reservas);
        model.addAttribute("quantidadePendentes", reservas.stream()
                .filter(reserva -> "SOLICITADA".equals(reserva.get("status")))
                .count());
        return "admin/reservas";
    }

    @PostMapping("/{reservaId}/aprovar")
    public String aprovar(
            Authentication authentication,
            @PathVariable UUID reservaId,
            RedirectAttributes redirect
    ) {
        reservaUseCases.aprovar(support.authenticatedUser(authentication), reservaId);
        redirect.addFlashAttribute("successMessage", "Reserva aprovada.");
        return "redirect:/admin/reservas";
    }

    @PostMapping("/{reservaId}/negar")
    public String negar(
            Authentication authentication,
            @PathVariable UUID reservaId,
            @RequestParam(defaultValue = "") String motivo,
            RedirectAttributes redirect
    ) {
        reservaUseCases.negar(support.authenticatedUser(authentication), reservaId, motivo);
        redirect.addFlashAttribute("successMessage", "Solicitação negada.");
        return "redirect:/admin/reservas";
    }

    private Map<String, Object> mapearReserva(Reserva reserva) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", reserva.getId());
        item.put("area", reserva.getAreaComum().getNome());
        item.put("morador", reserva.getMorador().getNome());
        item.put("email", reserva.getMorador().getEmail());
        item.put("inicio", formatar(reserva.getInicio()));
        item.put("fim", formatar(reserva.getFim()));
        item.put("criadaEm", formatar(reserva.getCriadaEm()));
        item.put("status", reserva.getStatus().name());
        item.put("motivoNegacao", reserva.getMotivoNegacao());
        item.put("decididaEm", formatar(reserva.getDecididaEm()));
        item.put("decididaPor", reserva.getDecididaPor() == null ? null : reserva.getDecididaPor().getNome());
        return item;
    }

    private String formatar(Instant instante) {
        if (instante == null) {
            return null;
        }
        return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(clock.getZone())
                .format(instante);
    }
}
