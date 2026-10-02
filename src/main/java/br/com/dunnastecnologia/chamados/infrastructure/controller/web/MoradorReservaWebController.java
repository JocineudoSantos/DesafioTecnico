package br.com.dunnastecnologia.chamados.infrastructure.controller.web;

import br.com.dunnastecnologia.chamados.application.UserCase.AreaComumUseCases;
import br.com.dunnastecnologia.chamados.application.UserCase.ReservaUseCases;
import br.com.dunnastecnologia.chamados.domain.model.ReservaStatus;
import br.com.dunnastecnologia.chamados.infrastructure.controller.web.form.SolicitarReservaForm;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/morador/reservas")
@PreAuthorize("hasRole('MORADOR')")
public class MoradorReservaWebController {

    private final AreaComumUseCases areaComumUseCases;
    private final ReservaUseCases reservaUseCases;
    private final WebControllerSupport support;
    private final Clock clock;

    public MoradorReservaWebController(
            AreaComumUseCases areaComumUseCases,
            ReservaUseCases reservaUseCases,
            WebControllerSupport support,
            Clock clock
    ) {
        this.areaComumUseCases = areaComumUseCases;
        this.reservaUseCases = reservaUseCases;
        this.support = support;
        this.clock = clock;
    }

    @ModelAttribute("solicitarReservaForm")
    public SolicitarReservaForm solicitarReservaForm() { return new SolicitarReservaForm(); }

    @GetMapping
    public String listar(
            Authentication authentication,
            @RequestParam(required = false) UUID areaId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime fim,
            @RequestParam(required = false) String mes,
            @RequestParam(name = "view", defaultValue = "lista") String visualizacao,
            Model model
    ) {
        var user = support.authenticatedUser(authentication);
        var areas = areaComumUseCases.listarAtivas(user);
        var reservas = reservaUseCases.listarMinhasReservas(user);
        model.addAttribute("pageTitle", "&Aacute;reas comuns e reservas");
        model.addAttribute("areasComuns", areas);
        YearMonth mesCalendario = new ReservaCalendarioModelBuilder(clock).mesSelecionado(mes);
        String modo = "calendario".equalsIgnoreCase(visualizacao) ? "calendario" : "lista";
        ReservaCalendarioModelBuilder calendario = new ReservaCalendarioModelBuilder(clock);
        model.addAttribute("diasCalendario", calendario.construirDias(reservas, mesCalendario, false));
        model.addAttribute("mesSelecionado", mesCalendario.toString());
        model.addAttribute("mesExibicao", calendario.rotuloMes(mesCalendario));
        model.addAttribute("mesAnterior", mesCalendario.minusMonths(1));
        model.addAttribute("mesSeguinte", mesCalendario.plusMonths(1));
        model.addAttribute("visualizacao", modo);
        model.addAttribute("minhasReservas", reservas.stream().map(reserva -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("area", reserva.getAreaComum().getNome());
            item.put("id", reserva.getId());
            item.put("inicio", DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(clock.getZone()).format(reserva.getInicio()));
            item.put("fim", DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(clock.getZone()).format(reserva.getFim()));
            item.put("criadaEm", DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(clock.getZone()).format(reserva.getCriadaEm()));
            item.put("status", reserva.getStatus().name());
            item.put("canceladaEm", reserva.getCanceladaEm() == null ? null : DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(clock.getZone()).format(reserva.getCanceladaEm()));
            item.put("canceladaPor", reserva.getCanceladaPor() == null ? null : reserva.getCanceladaPor().getNome());
            item.put("cancelavel", reserva.getInicio().isAfter(clock.instant())
                    && (reserva.getStatus() == ReservaStatus.SOLICITADA || reserva.getStatus() == ReservaStatus.APROVADA));
            return item;
        }).toList());
        model.addAttribute("areaSelecionadaId", areaId);
        model.addAttribute("inicioPesquisa", inicio);
        model.addAttribute("fimPesquisa", fim);
        if (areaId != null && inicio != null && fim != null) {
            var aprovadas = reservaUseCases.listarAprovadasSobrepostas(
                    user, areaId, inicio.atZone(clock.getZone()).toInstant(), fim.atZone(clock.getZone()).toInstant()
            );
            model.addAttribute("reservasAprovadasSobrepostas", aprovadas);
            model.addAttribute("consultaSemConflito", aprovadas.isEmpty());
        }
        return "morador/reservas/lista";
    }

    @PostMapping
    public String solicitar(Authentication authentication, @ModelAttribute SolicitarReservaForm form, RedirectAttributes redirect) {
        if (form.getInicio() == null || form.getFim() == null) {
            throw new IllegalArgumentException("Informe o início e o fim do período.");
        }
        reservaUseCases.solicitar(
                support.authenticatedUser(authentication),
                form.getAreaId(),
                form.getInicio().atZone(clock.getZone()).toInstant(),
                form.getFim().atZone(clock.getZone()).toInstant()
        );
        redirect.addFlashAttribute("successMessage", "Solicitação de reserva registrada como SOLICITADA.");
        return "redirect:/morador/reservas";
    }

    @PostMapping("/{reservaId}/cancelar")
    public String cancelar(Authentication authentication, @PathVariable UUID reservaId,
                           RedirectAttributes redirect) {
        reservaUseCases.cancelar(support.authenticatedUser(authentication), reservaId);
        redirect.addFlashAttribute("successMessage", "Reserva cancelada.");
        return "redirect:/morador/reservas";
    }
}
