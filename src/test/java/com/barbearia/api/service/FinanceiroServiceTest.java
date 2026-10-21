package com.barbearia.api.service;

import com.barbearia.api.domain.Agendamento;
import com.barbearia.api.domain.Barbeiro;
import com.barbearia.api.domain.Cliente;
import com.barbearia.api.domain.MovimentoFinanceiro;
import com.barbearia.api.domain.Servico;
import com.barbearia.api.domain.Usuario;
import com.barbearia.api.dto.financeiro.ResumoDia;
import com.barbearia.api.dto.financeiro.ResumoFinanceiroResponse;
import com.barbearia.api.repositories.MovimentoFinanceiroRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceiroServiceTest {

    @Mock
    private MovimentoFinanceiroRepository movimentoRepository;
    @Mock
    private BarbeiroLogado barbeiroLogado;
    @Mock
    private RelogioLocal relogio;

    @InjectMocks
    private FinanceiroService service;

    private static Agendamento concluido() {
        Servico corte = Servico.builder().preco(new BigDecimal("40.00")).custoInsumo(new BigDecimal("6.50"))
                .duracaoMinutos(30).build();
        Agendamento agendamento = Agendamento.novo(new Barbeiro(), new Cliente(), corte,
                LocalDateTime.of(2026, 10, 20, 10, 0));
        agendamento.setId(UUID.randomUUID());
        return agendamento;
    }

    @Test
    void conclusaoGeraMovimentoComOsValoresDoAgendamento() {
        Agendamento agendamento = concluido();
        ArgumentCaptor<MovimentoFinanceiro> salvo = ArgumentCaptor.forClass(MovimentoFinanceiro.class);

        service.registrarConclusao(agendamento);

        verify(movimentoRepository).save(salvo.capture());
        assertThat(salvo.getValue().getValorBruto()).isEqualByComparingTo("40.00");
        assertThat(salvo.getValue().getLucroLiquido()).isEqualByComparingTo("33.50");
        assertThat(salvo.getValue().getData()).isEqualTo(LocalDate.of(2026, 10, 20));
    }

    @Test
    void conclusaoRepetidaNaoDuplicaOMovimento() {
        Agendamento agendamento = concluido();
        when(movimentoRepository.existsByAgendamentoId(agendamento.getId())).thenReturn(true);

        service.registrarConclusao(agendamento);

        verify(movimentoRepository, never()).save(any());
    }

    @Test
    void resumoSomaOsDiasEPadraoEOMesAtual() {
        Usuario logado = new Usuario();
        Barbeiro barbeiro = Barbeiro.builder().id(UUID.randomUUID()).build();
        when(barbeiroLogado.de(logado)).thenReturn(barbeiro);
        when(relogio.agora()).thenReturn(LocalDateTime.of(2026, 10, 20, 12, 0));
        when(movimentoRepository.resumoPorDia(barbeiro.getId(), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 20)))
                .thenReturn(List.of(
                        new ResumoDia(LocalDate.of(2026, 10, 2), 2L, new BigDecimal("80.00"),
                                new BigDecimal("10.00"), new BigDecimal("70.00")),
                        new ResumoDia(LocalDate.of(2026, 10, 3), 1L, new BigDecimal("40.00"),
                                BigDecimal.ZERO, new BigDecimal("40.00"))));

        ResumoFinanceiroResponse resumo = service.resumo(logado, null, null);

        assertThat(resumo.atendimentos()).isEqualTo(3);
        assertThat(resumo.faturamento()).isEqualByComparingTo("120.00");
        assertThat(resumo.lucro()).isEqualByComparingTo("110.00");
        assertThat(resumo.dias()).hasSize(2);
    }
}
