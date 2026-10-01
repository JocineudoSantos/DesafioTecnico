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
                        <label class="field"><span>&Aacute;rea</span><select name="areaId" required>
                            <option value="">Selecione</option><c:forEach items="${areasComuns}" var="area"><option value="${area.id}" <c:if test="${area.id eq areaSelecionadaId}">selected</c:if>><c:out value="${area.nome}"/></option></c:forEach>
                        </select></label>
                        <label class="field"><span>In&iacute;cio</span><input type="datetime-local" name="inicio" required value="${inicioPesquisa}"></label>
                        <label class="field"><span>Fim</span><input type="datetime-local" name="fim" required value="${fimPesquisa}"></label>
                        <button type="submit" class="btn btn-secondary">Consultar</button>
                    </form>
                    <c:if test="${not empty reservasAprovadasSobrepostas}"><div class="alert alert-danger"><span>O per&iacute;odo tem conflito com reserva aprovada.</span></div></c:if>
                    <c:if test="${consultaSemConflito}"><div class="alert alert-success"><span>Sem conflito com reservas aprovadas. Solicita&ccedil;&otilde;es pendentes n&atilde;o bloqueiam o per&iacute;odo.</span></div>
                        <form method="post" action="${ctx}/morador/reservas" class="stack-form">
                            <%@ include file="/WEB-INF/jsp/fragments/csrf.jspf" %>
                            <input type="hidden" name="areaId" value="${areaSelecionadaId}"><input type="hidden" name="inicio" value="${inicioPesquisa}"><input type="hidden" name="fim" value="${fimPesquisa}">
                            <button type="submit" class="btn btn-primary">Solicitar reserva</button>
                        </form>
                    </c:if>
                </article>
                <article class="card">
                    <div class="section-header"><div><p class="eyebrow">Meu hist&oacute;rico</p><h2>Minhas reservas</h2></div></div>
                    <c:choose><c:when test="${empty minhasReservas}"><div class="empty-state"><h3>Nenhuma reserva solicitada</h3><p>Suas solicita&ccedil;&otilde;es aparecer&atilde;o aqui.</p></div></c:when>
                    <c:otherwise><div class="table-wrap"><table class="data-table"><thead><tr><th>&Aacute;rea</th><th>In&iacute;cio</th><th>Fim</th><th>Estado</th><th>Criada em</th></tr></thead><tbody>
                        <c:forEach items="${minhasReservas}" var="reserva"><tr><td><c:out value="${reserva.area}"/></td><td><c:out value="${reserva.inicio}"/></td><td><c:out value="${reserva.fim}"/></td><td><c:out value="${reserva.status}"/></td><td><c:out value="${reserva.criadaEm}"/></td></tr></c:forEach>
                    </tbody></table></div></c:otherwise></c:choose>
                </article>
            </section>
        </main>
    </div>
</div>
<%@ include file="/WEB-INF/jsp/fragments/scripts.jspf" %>
</body>
</html>
