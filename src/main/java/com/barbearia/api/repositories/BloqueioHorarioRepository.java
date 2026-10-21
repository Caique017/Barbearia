package com.barbearia.api.repositories;

import com.barbearia.api.domain.BloqueioHorario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BloqueioHorarioRepository extends JpaRepository<BloqueioHorario, UUID> {

    @Query("""
            select b from BloqueioHorario b
            where b.barbeiro.id = :barbeiroId and b.inicio < :fim and b.fim > :inicio
            order by b.inicio
            """)
    List<BloqueioHorario> sobrepostos(UUID barbeiroId, LocalDateTime inicio, LocalDateTime fim);

    List<BloqueioHorario> findByBarbeiroIdAndFimAfterOrderByInicio(UUID barbeiroId, LocalDateTime agora);

    Optional<BloqueioHorario> findByIdAndBarbeiroId(UUID id, UUID barbeiroId);
}
