package com.barbearia.api.repositories;

import com.barbearia.api.domain.Servico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServicoRepository extends JpaRepository<Servico, UUID> {

    List<Servico> findByBarbeiroIdOrderByNomeAsc(UUID barbeiroId);

    List<Servico> findByBarbeiroIdAndAtivoTrueOrderByNomeAsc(UUID barbeiroId);

    Optional<Servico> findByIdAndBarbeiroId(UUID id, UUID barbeiroId);

    boolean existsByBarbeiroIdAndNomeIgnoreCase(UUID barbeiroId, String nome);
}
