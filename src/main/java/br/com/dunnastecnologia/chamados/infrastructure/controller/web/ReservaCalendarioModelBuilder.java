package br.com.dunnastecnologia.chamados.infrastructure.controller.web;

import br.com.dunnastecnologia.chamados.domain.model.Reserva;
import br.com.dunnastecnologia.chamados.domain.model.ReservaStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Monta os dias e os eventos da agenda a partir das reservas já autorizadas ao perfil. */
public class ReservaCalendarioModelBuilder {

    private static final DateTimeFormatter PERIODO = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final DateTimeFormatter HORARIO = DateTimeFormatter.ofPattern("HH:mm");
    private final Clock clock;

    public ReservaCalendarioModelBuilder(Clock clock) {
        this.clock = clock;
    }

    public YearMonth mesSelecionado(String valor) {
        if (valor == null || valor.isBlank()) {
            return YearMonth.from(instanteAtual().atZone(clock.getZone()));
        }
        try {
            return YearMonth.parse(valor);
        } catch (RuntimeException exception) {
            return YearMonth.from(instanteAtual().atZone(clock.getZone()));
        }
    }

    public String rotuloMes(YearMonth mes) {
        String nome = mes.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-BR"));
        return nome.substring(0, 1).toUpperCase(Locale.forLanguageTag("pt-BR"))
                + nome.substring(1) + " de " + mes.getYear();
    }

    private Instant instanteAtual() {
        Instant instante = clock.instant();
        return instante == null ? Instant.now() : instante;
    }

    public List<Map<String, Object>> construirDias(List<Reserva> reservas, YearMonth mes, boolean administrador) {
        var zona = clock.getZone();
        LocalDate hoje = instanteAtual().atZone(zona).toLocalDate();
        LocalDate primeiroDia = mes.atDay(1);
        LocalDate inicioGrade = primeiroDia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<Map<String, Object>> dias = new ArrayList<>(42);

        for (int indice = 0; indice < 42; indice++) {
            LocalDate data = inicioGrade.plusDays(indice);
            Instant inicioDia = data.atStartOfDay(zona).toInstant();
            Instant fimDia = data.plusDays(1).atStartOfDay(zona).toInstant();
            List<Map<String, Object>> eventos = new ArrayList<>();

            for (Reserva reserva : reservas) {
                if (reserva.getStatus() != ReservaStatus.APROVADA
                        && reserva.getStatus() != ReservaStatus.SOLICITADA) {
                    continue;
                }
                if (!reserva.getInicio().isBefore(fimDia) || !reserva.getFim().isAfter(inicioDia)) {
                    continue;
                }
                Map<String, Object> evento = new LinkedHashMap<>();
                evento.put("area", reserva.getAreaComum().getNome());
                evento.put("status", reserva.getStatus().name());
                evento.put("periodo", PERIODO.withZone(zona).format(reserva.getInicio())
                        + " a " + PERIODO.withZone(zona).format(reserva.getFim()));
                evento.put("horario", HORARIO.withZone(zona).format(reserva.getInicio())
                        + "–" + HORARIO.withZone(zona).format(reserva.getFim()));
                if (administrador) {
                    evento.put("morador", reserva.getMorador().getNome());
                }
                eventos.add(evento);
            }

            Map<String, Object> dia = new LinkedHashMap<>();
            dia.put("numero", data.getDayOfMonth());
            dia.put("data", data);
            dia.put("noMes", YearMonth.from(data).equals(mes));
            dia.put("hoje", data.equals(hoje));
            dia.put("reservas", eventos);
            dias.add(dia);
        }
        return dias;
    }
}