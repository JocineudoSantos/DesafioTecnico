<%@ include file="/WEB-INF/jsp/fragments/taglibs.jspf" %>
<!DOCTYPE html>
<html lang="pt-BR">
<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>
<body data-page="admin-areas-comuns">
<div class="app-shell">
    <%@ include file="/WEB-INF/jsp/fragments/sidebar.jspf" %>
    <div class="app-main">
        <%@ include file="/WEB-INF/jsp/fragments/topbar.jspf" %>
        <main class="page-content">
            <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>
            <c:set var="areaAction" value="${ctx}/admin/areas-comuns" />
            <c:if test="${not empty areaComumEdicao}"><c:set var="areaAction" value="${ctx}/admin/areas-comuns/${areaComumEdicao.id}" /></c:if>
            <section class="two-column-grid">
                <article class="card">
                    <div class="section-header"><div><p class="eyebrow">Reservas</p><h2><c:choose><c:when test="${not empty areaComumEdicao}">Editar &aacute;rea comum</c:when><c:otherwise>Cadastrar &aacute;rea comum</c:otherwise></c:choose></h2></div></div>
                    <form method="post" action="${areaAction}" class="stack-form">
                        <%@ include file="/WEB-INF/jsp/fragments/csrf.jspf" %>
                        <label class="field"><span>Nome</span><input name="nome" maxlength="120" required value="<c:out value='${areaComumForm.nome}'/>" placeholder="Sal&atilde;o de festas"></label>
                        <label class="field"><span>Descri&ccedil;&atilde;o (opcional)</span><textarea name="descricao" maxlength="500" rows="3"><c:out value="${areaComumForm.descricao}"/></textarea></label>
                        <div class="button-row"><button type="submit" class="btn btn-primary"><c:choose><c:when test="${not empty areaComumEdicao}">Salvar altera&ccedil;&otilde;es</c:when><c:otherwise>Cadastrar &aacute;rea</c:otherwise></c:choose></button>
                            <c:if test="${not empty areaComumEdicao}"><a href="${ctx}/admin/areas-comuns" class="btn btn-secondary">Cancelar</a></c:if></div>
                    </form>
                </article>
                <article class="card">
                    <div class="section-header"><div><p class="eyebrow">Cadastro</p><h2>&Aacute;reas comuns</h2></div></div>
                    <c:choose><c:when test="${empty areasComuns}"><div class="empty-state"><h3>Nenhuma &aacute;rea cadastrada</h3><p>Cadastre uma &aacute;rea para permitir solicita&ccedil;&otilde;es de reserva.</p></div></c:when>
                    <c:otherwise><div class="table-wrap"><table class="data-table"><thead><tr><th>Nome</th><th>Descri&ccedil;&atilde;o</th><th>Situa&ccedil;&atilde;o</th><th>A&ccedil;&otilde;es</th></tr></thead><tbody>
                        <c:forEach items="${areasComuns}" var="area"><tr><td><c:out value="${area.nome}"/></td><td><c:out value="${area.descricao}"/></td><td><c:choose><c:when test="${area.ativa}">Ativa</c:when><c:otherwise>Inativa</c:otherwise></c:choose></td><td class="cell-actions">
                            <a href="${ctx}/admin/areas-comuns?areaId=${area.id}" class="btn btn-link">Editar</a>
                            <c:if test="${area.ativa}"><form method="post" action="${ctx}/admin/areas-comuns/${area.id}/desativar" onsubmit="return confirm('Desativar esta &aacute;rea? As reservas existentes ser&atilde;o preservadas.');">
                                <%@ include file="/WEB-INF/jsp/fragments/csrf.jspf" %><button type="submit" class="btn btn-link">Desativar</button></form></c:if>
                        </td></tr></c:forEach>
                    </tbody></table></div></c:otherwise></c:choose>
                </article>
            </section>
        </main>
    </div>
</div>
<%@ include file="/WEB-INF/jsp/fragments/scripts.jspf" %>
</body>
</html>
