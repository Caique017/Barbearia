package com.barbearia.api.barbeiro;

import com.barbearia.api.auth.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BarbeiroRepository extends JpaRepository<Barbeiro, UUID> {
    Optional<Barbeiro> findByUsuario(Usuario usuario);

    Optional<Barbeiro> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
