package com.barbearia.api.service;

import com.barbearia.api.domain.Agendamento;
import com.barbearia.api.domain.Barbeiro;
import com.barbearia.api.domain.MovimentoFinanceiro;
import com.barbearia.api.domain.Usuario;
import com.barbearia.api.dto.financeiro.ResumoDia;
import com.barbearia.api.dto.financeiro.ResumoFinanceiroResponse;
import com.barbearia.api.exceptions.HorarioInvalidoException;
import com.barbearia.api.repositories.MovimentoFinanceiroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class FinanceiroService {

    static final int MAXIMO_DIAS_NO_RESUMO = 366;

    private final MovimentoFinanceiroRepository movimentoRepository;
    private final BarbeiroLogado barbeiroLogado;
    private final RelogioLocal relogio;

    @Transactional
    public void registrarConclusao(Agendamento agendamento) {
        if (!movimentoRepository.existsByAgendamentoId(agendamento.getId())) {
            movimentoRepository.save(MovimentoFinanceiro.daConclusao(agendamento));
        }
    }

    @Transactional(readOnly = true)
    public ResumoFinanceiroResponse resumo(Usuario logado, LocalDate de, LocalDate ate) {
        Barbeiro barbeiro = barbeiroLogado.de(logado);
        LocalDate hoje = relogio.agora().toLocalDate();
        LocalDate inicio = de != null ? de : hoje.withDayOfMonth(1);
        LocalDate fim = ate != null ? ate : hoje;

        if (fim.isBefore(inicio) || ChronoUnit.DAYS.between(inicio, fim) >= MAXIMO_DIAS_NO_RESUMO) {
            throw new HorarioInvalidoException(
                    "Informe um período de até " + MAXIMO_DIAS_NO_RESUMO + " dias, com o fim depois do início.");
        }

        List<ResumoDia> dias = movimentoRepository.resumoPorDia(barbeiro.getId(), inicio, fim);

        return new ResumoFinanceiroResponse(
                inicio,
                fim,
                dias.stream().mapToLong(ResumoDia::atendimentos).sum(),
                somar(dias, ResumoDia::faturamento),
                somar(dias, ResumoDia::custos),
                somar(dias, ResumoDia::lucro),
                dias);
    }

    private static BigDecimal somar(List<ResumoDia> dias, Function<ResumoDia, BigDecimal> campo) {
        return dias.stream().map(campo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
