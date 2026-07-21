package com.barbearia.api.repositories;

import com.barbearia.api.domain.Barbeiro;
import com.barbearia.api.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BarbeiroRepository extends JpaRepository<Barbeiro, UUID> {
    Optional<Barbeiro> findByUsuario(Usuario usuario);
}
