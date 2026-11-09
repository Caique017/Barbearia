package com.barbearia.api.repositories;

import com.barbearia.api.domain.Agendamento;
import com.barbearia.api.enums.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgendamentoRepository extends JpaRepository<Agendamento, UUID> {

    List<StatusAgendamento> OCUPAM_HORARIO = List.of(StatusAgendamento.CONFIRMADO, StatusAgendamento.CONCLUIDO);

    @Query("""
            select a from Agendamento a
            where a.barbeiro.id = :barbeiroId and a.status in :status
              and a.dataHora < :fim and a.terminaEm > :inicio
            """)
    List<Agendamento> sobrepostos(UUID barbeiroId, LocalDateTime inicio, LocalDateTime fim,
                                  Collection<StatusAgendamento> status);

    @Query("""
            select a from Agendamento a join fetch a.cliente join fetch a.servico
            where a.barbeiro.id = :barbeiroId and a.dataHora >= :de and a.dataHora < :ate
            order by a.dataHora
            """)
    List<Agendamento> agendaDoBarbeiro(UUID barbeiroId, LocalDateTime de, LocalDateTime ate);

    @Query("""
            select a from Agendamento a join fetch a.servico
            where a.cliente.id = :clienteId
            order by a.dataHora desc
            """)
    List<Agendamento> doCliente(UUID clienteId);

    @Query("""
            select a from Agendamento a join fetch a.cliente join fetch a.servico
            where a.barbeiro.id = :barbeiroId
            order by a.dataHora desc
            """)
    List<Agendamento> todosDoBarbeiro(UUID barbeiroId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Agendamento a set a.status = com.barbearia.api.enums.StatusAgendamento.CANCELADO
            where a.cliente.id = :clienteId
              and a.status = com.barbearia.api.enums.StatusAgendamento.CONFIRMADO
              and a.dataHora > :agora
            """)
    int cancelarFuturosDoCliente(UUID clienteId, LocalDateTime agora);

    long countByClienteIdAndStatusAndDataHoraAfter(UUID clienteId, StatusAgendamento status, LocalDateTime depois);

    Optional<Agendamento> findByIdAndBarbeiroId(UUID id, UUID barbeiroId);

    Optional<Agendamento> findByIdAndClienteId(UUID id, UUID clienteId);
}
