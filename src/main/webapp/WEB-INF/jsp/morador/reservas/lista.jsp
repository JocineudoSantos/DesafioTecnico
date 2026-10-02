<%@ include file="/WEB-INF/jsp/fragments/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>
<body data-page="morador-reservas">
<div class="app-shell">
    <%@ include file="/WEB-INF/jsp/fragments/sidebar.jspf" %>
    <div class="app-main">
        <%@ include file="/WEB-INF/jsp/fragments/topbar.jspf" %>
        <main class="page-content">
            <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>
            <section class="two-column-grid">
                <article class="card">
                    <div class="section-header"><div><p class="eyebrow">&Aacute;reas comuns</p><h2>Consultar disponibilidade</h2></div></div>
                    <form method="get" action="${ctx}/morador/reservas" class="stack-form">
                        <label class="field"><span>&Aacute;rea</span><select name="areaId" required><option value="">Selecione</option><c:forEach items="${areasComuns}" var="area"><option value="${area.id}" <c:if test="${area.id eq areaSelecionadaId}">selected</c:if>><c:out value="${area.nome}"/></option></c:forEach></select></label>
                        <label class="field"><span>In&iacute;cio</span><input type="datetime-local" name="inicio" required value="${inicioPesquisa}"></label>
                        <label class="field"><span>Fim</span><input type="datetime-local" name="fim" required value="${fimPesquisa}"></label>
                        <button type="submit" class="btn btn-secondary">Consultar</button>
                    </form>
                    <c:if test="${not empty reservasAprovadasSobrepostas}"><div class="alert alert-danger"><span>O per&iacute;odo tem conflito com reserva aprovada.</span></div></c:if>
                    <c:if test="${consultaSemConflito}"><div class="alert alert-success"><span>Sem conflito com reservas aprovadas. Solicita&ccedil;&otilde;es pendentes n&atilde;o bloqueiam o per&iacute;odo.</span></div><form method="post" action="${ctx}/morador/reservas" class="stack-form"><%@ include file="/WEB-INF/jsp/fragments/csrf.jspf" %><input type="hidden" name="areaId" value="${areaSelecionadaId}"><input type="hidden" name="inicio" value="${inicioPesquisa}"><input type="hidden" name="fim" value="${fimPesquisa}"><button type="submit" class="btn btn-primary">Solicitar reserva</button></form></c:if>
                </article>
                <article class="card">
                    <div class="section-header"><div><p class="eyebrow">Meu hist&oacute;rico</p><h2>Minhas reservas</h2></div></div>
                    <nav class="calendar-toolbar" aria-label="Visualiza&ccedil;&atilde;o das minhas reservas">
                        <div class="calendar-view-switch"><a class="btn btn-secondary" href="${ctx}/morador/reservas?view=lista">Lista</a><a class="btn btn-secondary" href="${ctx}/morador/reservas?view=calendario&amp;mes=${mesSelecionado}">Calend&aacute;rio</a></div>
                        <c:if test="${visualizacao eq 'calendario'}"><div class="calendar-month-switch"><a class="btn btn-secondary" href="${ctx}/morador/reservas?view=calendario&amp;mes=${mesAnterior}">Anterior</a><strong><c:out value="${mesExibicao}"/></strong><a class="btn btn-secondary" href="${ctx}/morador/reservas?view=calendario&amp;mes=${mesSeguinte}">Pr&oacute;ximo</a></div></c:if>
                    </nav>
                    <c:choose>
                        <c:when test="${visualizacao eq 'calendario'}">
                            <p class="calendar-caption">A agenda exibe somente suas solicita&ccedil;&otilde;es pendentes e reservas aprovadas.</p>
                            <div class="calendar-grid" role="grid" aria-label="Agenda mensal das minhas reservas">
                                <div class="calendar-weekday">Segunda</div><div class="calendar-weekday">Ter&ccedil;a</div><div class="calendar-weekday">Quarta</div><div class="calendar-weekday">Quinta</div><div class="calendar-weekday">Sexta</div><div class="calendar-weekday">S&aacute;bado</div><div class="calendar-weekday">Domingo</div>
                                <c:forEach items="${diasCalendario}" var="dia"><div class="calendar-day <c:if test='${not dia.noMes}'>calendar-day-outside</c:if> <c:if test='${dia.hoje}'>calendar-day-today</c:if>" role="gridcell"><span class="calendar-day-number"><c:out value="${dia.numero}"/></span><c:forEach items="${dia.reservas}" var="reserva"><div class="calendar-event <c:choose><c:when test='${reserva.status eq "APROVADA"}'>calendar-event-approved</c:when><c:otherwise>calendar-event-pending</c:otherwise></c:choose>" title="${reserva.periodo}"><strong><c:out value="${reserva.area}"/></strong><span><c:out value="${reserva.horario}"/> · <c:out value="${reserva.status}"/></span></div></c:forEach></div></c:forEach>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <c:choose><c:when test="${empty minhasReservas}"><div class="empty-state"><h3>Nenhuma reserva solicitada</h3><p>Suas solicita&ccedil;&otilde;es aparecer&atilde;o aqui.</p></div></c:when>
                            <c:otherwise><div class="table-wrap"><table class="data-table"><thead><tr><th>&Aacute;rea</th><th>In&iacute;cio</th><th>Fim</th><th>Estado</th><th>Criada em</th></tr></thead><tbody><c:forEach items="${minhasReservas}" var="reserva"><tr><td><c:out value="${reserva.area}"/></td><td><c:out value="${reserva.inicio}"/></td><td><c:out value="${reserva.fim}"/></td><td><c:out value="${reserva.status}"/></td><td><c:out value="${reserva.criadaEm}"/></td></tr></c:forEach></tbody></table></div></c:otherwise></c:choose>
                        </c:otherwise>
                    </c:choose>
                </article>
            </section>
        </main>
    </div>
</div>
<%@ include file="/WEB-INF/jsp/fragments/scripts.jspf" %>
</body>
</html>