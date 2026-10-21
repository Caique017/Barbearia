package com.barbearia.api.repositories;

import com.barbearia.api.domain.MovimentoFinanceiro;
import com.barbearia.api.dto.financeiro.ResumoDia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MovimentoFinanceiroRepository extends JpaRepository<MovimentoFinanceiro, UUID> {

    boolean existsByAgendamentoId(UUID agendamentoId);

    @Query("""
            select new com.barbearia.api.dto.financeiro.ResumoDia(
                m.data, count(m), sum(m.valorBruto), sum(m.custoInsumo), sum(m.lucroLiquido))
            from MovimentoFinanceiro m
            where m.agendamento.barbeiro.id = :barbeiroId and m.data between :de and :ate
            group by m.data
            order by m.data
            """)
    List<ResumoDia> resumoPorDia(UUID barbeiroId, LocalDate de, LocalDate ate);
}
