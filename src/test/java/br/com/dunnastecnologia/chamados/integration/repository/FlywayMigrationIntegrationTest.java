package br.com.dunnastecnologia.chamados.integration.repository;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
class FlywayMigrationIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    void aplicaTodasAsMigracoesEmPostgresVazioIncluindoAuditoriaDoCancelamento() {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load();

        var resultado = flyway.migrate();

        assertEquals(21, resultado.migrationsExecuted);
        assertEquals("21", flyway.info().current().getVersion().getVersion());
    }
}
