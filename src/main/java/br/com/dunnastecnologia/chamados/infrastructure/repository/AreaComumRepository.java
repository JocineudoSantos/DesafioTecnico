package br.com.dunnastecnologia.chamados.infrastructure.repository;

import br.com.dunnastecnologia.chamados.domain.model.AreaComum;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AreaComumRepository extends JpaRepository<AreaComum, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AreaComum a where a.id = :id")
    Optional<AreaComum> findByIdForUpdate(@Param("id") UUID id);

    List<AreaComum> findAllByOrderByNomeAsc();

    List<AreaComum> findByAtivaTrueOrderByNomeAsc();

    Optional<AreaComum> findByIdAndAtivaTrue(UUID id);
}
