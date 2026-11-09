package com.barbearia.api.repositories;

import com.barbearia.api.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByEmail(String email);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Usuario u where u.id in :ids")
    void apagarTodos(Collection<UUID> ids);
}
