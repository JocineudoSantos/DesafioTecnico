package br.com.dunnastecnologia.chamados.integration.repository;

import br.com.dunnastecnologia.chamados.domain.model.AreaComum;
import br.com.dunnastecnologia.chamados.domain.model.Administrador;
import br.com.dunnastecnologia.chamados.domain.model.Morador;
import br.com.dunnastecnologia.chamados.domain.model.Reserva;
import br.com.dunnastecnologia.chamados.domain.model.ReservaStatus;
import br.com.dunnastecnologia.chamados.infrastructure.repository.ReservaRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.AdministradorRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.AreaComumRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.MoradorRepository;
import br.com.dunnastecnologia.chamados.infrastructure.service.ReservaService;
import br.com.dunnastecnologia.chamados.infrastructure.service.support.AuthenticatedUserValidator;
import br.com.dunnastecnologia.chamados.infrastructure.exception.BusinessRuleException;
import br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import java.util.concurrent.Executors;
import java.time.Clock;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

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
    @Autowired AreaComumRepository areaRepository;
    @Autowired MoradorRepository moradorRepository;
    @Autowired AdministradorRepository administradorRepository;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactionManager;

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

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void aprovacoesSimultaneasConflitantesNaoGeramDuasReservasAprovadas() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        Fixture fixture = tx.execute(status -> criarFixtureConcorrente());
        ReservaService service = new ReservaService(
                repository,
                areaRepository,
                moradorRepository,
                administradorRepository,
                mock(AuthenticatedUserValidator.class),
                Clock.fixed(Instant.parse("2030-01-01T08:00:00Z"), ZoneOffset.UTC)
        );
        AuthenticatedUser adminUser = new AuthenticatedUser(fixture.adminId(), "admin@test.local", "ROLE_ADMINISTRADOR");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> first = executor.submit(() -> tentarAprovar(
                    tx, service, adminUser, fixture.firstReservationId(), ready, start));
            Future<Boolean> second = executor.submit(() -> tentarAprovar(
                    tx, service, adminUser, fixture.secondReservationId(), ready, start));
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            assertEquals(1, List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS))
                    .stream().filter(Boolean::booleanValue).count());
        }

        assertEquals(1L, tx.execute(status -> repository.findAll().stream()
                .filter(reserva -> reserva.getStatus() == ReservaStatus.APROVADA).count()).longValue());
    }

    private boolean tentarAprovar(
            TransactionTemplate tx,
            ReservaService service,
            AuthenticatedUser admin,
            UUID reservaId,
            CountDownLatch ready,
            CountDownLatch start
    ) throws Exception {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Início concorrente não liberado");
        }
        try {
            tx.execute(status -> service.aprovar(admin, reservaId));
            return true;
        } catch (BusinessRuleException conflitoEsperado) {
            return false;
        }
    }

    private Fixture criarFixtureConcorrente() {
        AreaComum area = area("Salão concorrente");
        Administrador administrador = new Administrador();
        administrador.setNome("Administrador");
        administrador.setEmail("admin-" + UUID.randomUUID() + "@test.local");
        administrador.setSenha("hash");
        administrador.setAtivo(true);
        entityManager.persist(administrador);
        Morador primeiroMorador = morador();
        Morador segundoMorador = morador();
        Instant inicio = Instant.parse("2030-01-02T10:00:00Z");
        Reserva primeira = novaReserva(area, primeiroMorador, inicio);
        Reserva segunda = novaReserva(area, segundoMorador, inicio.plusSeconds(1800));
        entityManager.persist(primeira);
        entityManager.persist(segunda);
        entityManager.flush();
        return new Fixture(administrador.getId(), primeira.getId(), segunda.getId());
    }

    private Reserva novaReserva(AreaComum area, Morador morador, Instant inicio) {
        Reserva reserva = new Reserva();
        reserva.setAreaComum(area);
        reserva.setMorador(morador);
        reserva.setInicio(inicio);
        reserva.setFim(inicio.plusSeconds(3600));
        reserva.setStatus(ReservaStatus.SOLICITADA);
        reserva.setCriadaEm(inicio.minusSeconds(3600));
        return reserva;
    }

    private record Fixture(UUID adminId, UUID firstReservationId, UUID secondReservationId) { }

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
