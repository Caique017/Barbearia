package com.barbearia.api.repositories;

import com.barbearia.api.domain.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    Optional<Cliente> findByTelefoneAndBarbeiroId(String telefone, UUID barbeiroId);

    boolean existsByTelefoneAndBarbeiroId(String telefone, UUID barbeiroId);

    List<Cliente> findByBarbeiroIdOrderByNomeAsc(UUID barbeiroId);

    Optional<Cliente> findByIdAndBarbeiroId(UUID id, UUID barbeiroId);
}
