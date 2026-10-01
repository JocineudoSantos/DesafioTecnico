package br.com.dunnastecnologia.chamados.infrastructure.repository;

import br.com.dunnastecnologia.chamados.domain.model.AreaComum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AreaComumRepository extends JpaRepository<AreaComum, UUID> {
    List<AreaComum> findAllByOrderByNomeAsc();

    List<AreaComum> findByAtivaTrueOrderByNomeAsc();

    Optional<AreaComum> findByIdAndAtivaTrue(UUID id);
}
