package br.com.dunnastecnologia.chamados.infrastructure.repository;

import br.com.dunnastecnologia.chamados.domain.model.Reserva;
import br.com.dunnastecnologia.chamados.domain.model.ReservaStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservaRepository extends JpaRepository<Reserva, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.id = :id")
    Optional<Reserva> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select r from Reserva r
            join fetch r.areaComum
            join fetch r.morador
            left join fetch r.decididaPor
            order by r.inicio, r.criadaEm
            """)
    List<Reserva> listarTodasParaAdministracao();

    @Query("""
            select (count(r) > 0)
            from Reserva r
            where r.areaComum.id = :areaId
              and r.status = :status
              and r.inicio < :fim
              and r.fim > :inicio
            """)
    boolean existeSobreposicao(
            @Param("areaId") UUID areaId,
            @Param("status") ReservaStatus status,
            @Param("inicio") Instant inicio,
            @Param("fim") Instant fim
    );

    @Query("""
            select r from Reserva r
            join fetch r.areaComum
            left join fetch r.decididaPor
            where r.morador.id = :moradorId
            order by r.inicio desc
            """)
    List<Reserva> listarDoMorador(@Param("moradorId") UUID moradorId);

    @Query("""
            select r from Reserva r
            where r.areaComum.id = :areaId
              and r.status = :status
              and r.inicio < :fim
              and r.fim > :inicio
            order by r.inicio
            """)
    List<Reserva> listarSobreposicoes(
            @Param("areaId") UUID areaId,
            @Param("status") ReservaStatus status,
            @Param("inicio") Instant inicio,
            @Param("fim") Instant fim
    );
}
