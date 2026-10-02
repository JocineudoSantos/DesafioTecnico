<%@ include file="/WEB-INF/jsp/fragments/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>
<body data-page="admin-reservas">
<div class="app-shell">
    <%@ include file="/WEB-INF/jsp/fragments/sidebar.jspf" %>
    <div class="app-main">
        <%@ include file="/WEB-INF/jsp/fragments/topbar.jspf" %>
        <main class="page-content">
            <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>
            <section class="card">
                <div class="section-header">
                    <div>
                        <p class="eyebrow">&Aacute;reas comuns</p>
                        <h2>Reservas</h2>
                    </div>
                    <span class="status-pill">Pendentes: <c:out value="${quantidadePendentes}"/></span>
                </div>
                <nav class="calendar-toolbar" aria-label="Visualiza&ccedil;&atilde;o das reservas">
                    <div class="calendar-view-switch">
                        <a class="btn btn-secondary" href="${ctx}/admin/reservas?view=lista">Lista</a>
                        <a class="btn btn-secondary" href="${ctx}/admin/reservas?view=calendario&amp;mes=${mesSelecionado}">Calend&aacute;rio</a>
                    </div>
                    <c:if test="${visualizacao eq 'calendario'}">
                        <div class="calendar-month-switch">
                            <a class="btn btn-secondary" href="${ctx}/admin/reservas?view=calendario&amp;mes=${mesAnterior}">Anterior</a>
                            <strong><c:out value="${mesExibicao}"/></strong>
                            <a class="btn btn-secondary" href="${ctx}/admin/reservas?view=calendario&amp;mes=${mesSeguinte}">Pr&oacute;ximo</a>
                        </div>
                    </c:if>
                </nav>

                <c:choose>
                    <c:when test="${visualizacao eq 'calendario'}">
                        <p class="calendar-caption">A agenda mostra solicita&ccedil;&otilde;es pendentes e reservas aprovadas.</p>
                        <div class="calendar-grid" role="grid" aria-label="Agenda mensal de reservas">
                            <div class="calendar-weekday">Segunda</div><div class="calendar-weekday">Ter&ccedil;a</div>
                            <div class="calendar-weekday">Quarta</div><div class="calendar-weekday">Quinta</div>
                            <div class="calendar-weekday">Sexta</div><div class="calendar-weekday">S&aacute;bado</div>
                            <div class="calendar-weekday">Domingo</div>
                            <c:forEach items="${diasCalendario}" var="dia">
                                <div class="calendar-day <c:if test='${not dia.noMes}'>calendar-day-outside</c:if> <c:if test='${dia.hoje}'>calendar-day-today</c:if>" role="gridcell">
                                    <span class="calendar-day-number"><c:out value="${dia.numero}"/></span>
                                    <c:forEach items="${dia.reservas}" var="reserva">
                                        <div class="calendar-event <c:choose><c:when test='${reserva.status eq "APROVADA"}'>calendar-event-approved</c:when><c:otherwise>calendar-event-pending</c:otherwise></c:choose>" title="${reserva.periodo}">
                                            <strong><c:out value="${reserva.area}"/></strong>
                                            <span><c:out value="${reserva.horario}"/> · <c:out value="${reserva.status}"/></span>
                                            <span><c:out value="${reserva.morador}"/></span>
                                        </div>
                                    </c:forEach>
                                </div>
                            </c:forEach>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <c:choose>
                            <c:when test="${empty reservas}">
                                <div class="empty-state"><h3>Nenhuma reserva encontrada</h3><p>As solicita&ccedil;&otilde;es dos moradores aparecer&atilde;o aqui.</p></div>
                            </c:when>
                            <c:otherwise>
                                <div class="table-wrap">
                                    <table class="data-table">
                                        <thead><tr><th>&Aacute;rea e per&iacute;odo</th><th>Morador</th><th>Situa&ccedil;&atilde;o</th><th>Decis&atilde;o</th></tr></thead>
                                        <tbody>
                                        <c:forEach items="${reservas}" var="reserva">
                                            <tr>
                                                <td><strong><c:out value="${reserva.area}"/></strong><br><span><c:out value="${reserva.inicio}"/> a <c:out value="${reserva.fim}"/></span><br><span>Solicitada em: <c:out value="${reserva.criadaEm}"/></span></td>
                                                <td><strong><c:out value="${reserva.morador}"/></strong><br><span><c:out value="${reserva.email}"/></span></td>
                                                <td><strong><c:out value="${reserva.status}"/></strong><c:if test="${reserva.status eq 'NEGADA' and not empty reserva.motivoNegacao}"><br><span><c:out value="${reserva.motivoNegacao}"/></span></c:if><c:if test="${reserva.status eq 'CANCELADA'}"><br><span>Cancelada em: <c:out value="${reserva.canceladaEm}"/></span><br><span>Cancelada por: <c:out value="${reserva.canceladaPor}"/></span></c:if></td>
                                                                                                <td class="cell-actions reservation-cell-actions">
                                                    <c:choose>
                                                        <c:when test="${reserva.status eq 'SOLICITADA'}">
                                                            <div class="reservation-actions">
                                                                <div class="reservation-action-buttons">
                                                                    <form method="post" action="${ctx}/admin/reservas/${reserva.id}/aprovar" class="reservation-approve-form">
                                                                        <%@ include file="/WEB-INF/jsp/fragments/csrf.jspf" %>
                                                                        <button type="submit" class="btn btn-primary">Aprovar</button>
                                                                    </form>
                                                                    <button type="button" class="btn btn-danger reservation-denial-toggle" data-reservation-denial-toggle aria-expanded="false" aria-controls="reservation-denial-${reserva.id}">Negar</button>
                                                                </div>
                                                                <form id="reservation-denial-${reserva.id}" method="post" action="${ctx}/admin/reservas/${reserva.id}/negar" class="stack-form compact-form reservation-denial-form" data-reservation-denial-form hidden>
                                                                    <%@ include file="/WEB-INF/jsp/fragments/csrf.jspf" %>
                                                                    <label class="field">
                                                                        <span>Motivo da negativa</span>
                                                                        <textarea name="motivo" rows="2" required></textarea>
                                                                    </label>
                                                                    <button type="submit" class="btn btn-danger">Confirmar negativa</button>
                                                                </form>
                                                            </div>
                                                        </c:when>
                                                        <c:otherwise><c:if test="${not empty reserva.decididaPor}"><strong>Respons&aacute;vel:</strong> <c:out value="${reserva.decididaPor}"/><br><strong>Data:</strong> <c:out value="${reserva.decididaEm}"/></c:if><c:if test="${reserva.cancelavel}"><form method="post" action="${ctx}/admin/reservas/${reserva.id}/cancelar" data-confirm="Cancelar esta reserva?" class="reservation-cancel-form"><%@ include file="/WEB-INF/jsp/fragments/csrf.jspf" %><button type="submit" class="btn btn-danger">Cancelar reserva</button></form></c:if></c:otherwise>
                                                    </c:choose>
                                                </td>
                                            </tr>
                                        </c:forEach>
                                        </tbody>
                                    </table>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </c:otherwise>
                </c:choose>
            </section>
        </main>
    </div>
</div>
<%@ include file="/WEB-INF/jsp/fragments/scripts.jspf" %>
</body>
</html>
