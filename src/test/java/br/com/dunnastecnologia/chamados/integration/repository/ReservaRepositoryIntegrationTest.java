package br.com.dunnastecnologia.chamados.integration.repository;

import br.com.dunnastecnologia.chamados.domain.model.AreaComum;
import br.com.dunnastecnologia.chamados.domain.model.Morador;
import br.com.dunnastecnologia.chamados.domain.model.Reserva;
import br.com.dunnastecnologia.chamados.domain.model.ReservaStatus;
import br.com.dunnastecnologia.chamados.infrastructure.repository.ReservaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReservaRepositoryIntegrationTest {
    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired ReservaRepository repository;
    @Autowired EntityManager entityManager;

    @Test
    void sobreposicaoContaApenasAprovadasDaMesmaAreaEUsaIntervaloSemiAberto() {
        AreaComum area = area("Salão");
        AreaComum outraArea = area("Quadra");
        Morador morador = morador();
        Instant inicio = Instant.parse("2030-01-01T10:00:00Z");
        persistirReserva(area, morador, inicio, inicio.plusSeconds(3600), ReservaStatus.APROVADA);
        persistirReserva(area, morador, inicio.plusSeconds(7200), inicio.plusSeconds(10800), ReservaStatus.SOLICITADA);
        persistirReserva(outraArea, morador, inicio, inicio.plusSeconds(3600), ReservaStatus.APROVADA);
        entityManager.flush();
        entityManager.clear();

        assertTrue(repository.existeSobreposicao(area.getId(), ReservaStatus.APROVADA,
                inicio.plusSeconds(1800), inicio.plusSeconds(5400)));
        assertFalse(repository.existeSobreposicao(area.getId(), ReservaStatus.APROVADA,
                inicio.plusSeconds(3600), inicio.plusSeconds(7200)));
        assertFalse(repository.existeSobreposicao(area.getId(), ReservaStatus.APROVADA,
                inicio.plusSeconds(7200), inicio.plusSeconds(10800)));
    }

    private AreaComum area(String nome) {
        AreaComum area = new AreaComum();
        area.setNome(nome);
        area.setAtiva(true);
        entityManager.persist(area);
        return area;
    }

    private Morador morador() {
        Morador morador = new Morador();
        morador.setNome("Morador");
        morador.setEmail(UUID.randomUUID() + "@test.local");
        morador.setSenha("hash");
        morador.setAtivo(true);
        entityManager.persist(morador);
        return morador;
    }

    private void persistirReserva(AreaComum area, Morador morador, Instant inicio, Instant fim, ReservaStatus status) {
        Reserva reserva = new Reserva();
        reserva.setAreaComum(area);
        reserva.setMorador(morador);
        reserva.setInicio(inicio);
        reserva.setFim(fim);
        reserva.setStatus(status);
        reserva.setCriadaEm(inicio.minusSeconds(7200));
        entityManager.persist(reserva);
    }
}
