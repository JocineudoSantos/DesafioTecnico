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
                        <h2>Solicita&ccedil;&otilde;es de reserva</h2>
                    </div>
                    <span class="status-pill">Pendentes: <c:out value="${quantidadePendentes}"/></span>
                </div>

                <c:choose>
                    <c:when test="${empty reservas}">
                        <div class="empty-state">
                            <h3>Nenhuma reserva encontrada</h3>
                            <p>As solicita&ccedil;&otilde;es dos moradores aparecer&atilde;o aqui.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="table-wrap">
                            <table class="data-table">
                                <thead>
                                <tr>
                                    <th>&Aacute;rea e per&iacute;odo</th>
                                    <th>Morador</th>
                                    <th>Situa&ccedil;&atilde;o</th>
                                    <th>Decis&atilde;o</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach items="${reservas}" var="reserva">
                                    <tr>
                                        <td>
                                            <strong><c:out value="${reserva.area}"/></strong><br>
                                            <span><c:out value="${reserva.inicio}"/> a <c:out value="${reserva.fim}"/></span><br>
                                            <span>Solicitada em: <c:out value="${reserva.criadaEm}"/></span>
                                        </td>
                                        <td>
                                            <strong><c:out value="${reserva.morador}"/></strong><br>
                                            <span><c:out value="${reserva.email}"/></span>
                                        </td>
                                        <td>
                                            <strong><c:out value="${reserva.status}"/></strong>
                                            <c:if test="${reserva.status eq 'NEGADA' and not empty reserva.motivoNegacao}">
                                                <br><span><c:out value="${reserva.motivoNegacao}"/></span>
                                            </c:if>
                                        </td>
                                        <td class="cell-actions">
                                            <c:choose>
                                                <c:when test="${reserva.status eq 'SOLICITADA'}">
                                                    <form method="post" action="${ctx}/admin/reservas/${reserva.id}/aprovar">
                                                        <%@ include file="/WEB-INF/jsp/fragments/csrf.jspf" %>
                                                        <button type="submit" class="btn btn-primary">Aprovar</button>
                                                    </form>
                                                    <form method="post" action="${ctx}/admin/reservas/${reserva.id}/negar" class="stack-form compact-form reservation-denial-form">
                                                        <%@ include file="/WEB-INF/jsp/fragments/csrf.jspf" %>
                                                        <label class="field">
                                                            <span>Motivo da negativa</span>
                                                            <textarea name="motivo" rows="2" required></textarea>
                                                        </label>
                                                        <button type="submit" class="btn btn-danger">Negar</button>
                                                    </form>
                                                </c:when>
                                                <c:otherwise>
                                                    <strong>Respons&aacute;vel:</strong> <c:out value="${reserva.decididaPor}"/><br>
                                                    <strong>Data:</strong> <c:out value="${reserva.decididaEm}"/>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>
        </main>
    </div>
</div>
<%@ include file="/WEB-INF/jsp/fragments/scripts.jspf" %>
</body>
</html>
