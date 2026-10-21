package com.barbearia.api.repositories;

import com.barbearia.api.domain.BarbeiroHorario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

public interface BarbeiroHorarioRepository extends JpaRepository<BarbeiroHorario, UUID> {

    @Query("select h from BarbeiroHorario h where h.barbeiro.id = :barbeiroId order by h.diaSemana, h.abre")
    List<BarbeiroHorario> doBarbeiro(UUID barbeiroId);

    List<BarbeiroHorario> findByBarbeiroIdAndDiaSemanaOrderByAbre(UUID barbeiroId, DayOfWeek diaSemana);

    @Modifying
    @Query("delete from BarbeiroHorario h where h.barbeiro.id = :barbeiroId")
    void apagarDoBarbeiro(UUID barbeiroId);
}
