package br.com.dunnastecnologia.chamados.integration.repository;

import br.com.dunnastecnologia.chamados.domain.model.Chamado;
import br.com.dunnastecnologia.chamados.domain.model.StatusChamado;
import br.com.dunnastecnologia.chamados.domain.model.TipoChamado;
import br.com.dunnastecnologia.chamados.infrastructure.repository.ChamadoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:chamado-atraso;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
class ChamadoRepositoryIntegrationTest {

    @Autowired
    private ChamadoRepository chamadoRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void marcarChamadosAtrasadosDeveAtualizarStatusQuandoSlaJaEstiverExpirado() {
        StatusChamado solicitado = new StatusChamado();
        solicitado.setNome("Solicitado");
        solicitado.setInicialPadrao(Boolean.TRUE);
        entityManager.persist(solicitado);

        StatusChamado atrasado = new StatusChamado();
        atrasado.setNome("Atrasado");
        atrasado.setInicialPadrao(Boolean.FALSE);
        entityManager.persist(atrasado);

        TipoChamado tipoChamado = new TipoChamado();
        tipoChamado.setTitulo("Eletrica");
        tipoChamado.setPrazoHoras(1);
        entityManager.persist(tipoChamado);

        Chamado chamado = new Chamado();
        chamado.setDescricao("Luz apagada");
        chamado.setDataAbertura(LocalDateTime.now().minusHours(2));
        chamado.setStatus(solicitado);
        chamado.setTipoChamado(tipoChamado);
        entityManager.persist(chamado);

        entityManager.flush();
        entityManager.clear();

        int registrosAtualizados = chamadoRepository.marcarChamadosAtrasados();

        entityManager.clear();
        Chamado chamadoAtualizado = entityManager.find(Chamado.class, chamado.getId());

        assertEquals(1, registrosAtualizados);
        assertEquals("Atrasado", chamadoAtualizado.getStatus().getNome());
    }
}
