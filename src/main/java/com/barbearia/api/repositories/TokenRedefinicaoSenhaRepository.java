package com.barbearia.api.repositories;

import com.barbearia.api.domain.TokenRedefinicaoSenha;
import com.barbearia.api.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TokenRedefinicaoSenhaRepository extends JpaRepository<TokenRedefinicaoSenha, UUID> {
    Optional<TokenRedefinicaoSenha> findByTokenHash(String tokenHash);

    boolean existsByUsuarioAndCriadoEmAfter(Usuario usuario, Instant limite);

    @Modifying
    @Query("update TokenRedefinicaoSenha t set t.usadoEm = :agora where t.usuario = :usuario and t.usadoEm is null")
    int invalidarPendentes(Usuario usuario, Instant agora);

    @Modifying
    @Query("update TokenRedefinicaoSenha t set t.usadoEm = :agora where t.id = :id and t.usadoEm is null")
    int marcarUsado(UUID id, Instant agora);

    @Modifying
    @Query("delete from TokenRedefinicaoSenha t where t.expiraEm < :limite or t.usadoEm < :limite")
    int apagarAnterioresA(Instant limite);
}
