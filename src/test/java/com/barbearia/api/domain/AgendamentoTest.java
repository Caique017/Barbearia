package com.barbearia.api.domain;

import com.barbearia.api.enums.StatusAgendamento;
import com.barbearia.api.exceptions.TransicaoAgendamentoInvalidaException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgendamentoTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 10, 21, 10, 0);

    private static Agendamento agendamento() {
        Servico corte = Servico.builder().nome("Corte").preco(new BigDecimal("40.00"))
                .custoInsumo(new BigDecimal("5.00")).duracaoMinutos(45).build();
        return Agendamento.novo(new Barbeiro(), new Cliente(), corte, INICIO);
    }

    @Test
    void novoCopiaPrecoECustoEcalculaOFim() {
        Agendamento agendamento = agendamento();

        assertThat(agendamento.getTerminaEm()).isEqualTo(INICIO.plusMinutes(45));
        assertThat(agendamento.getPreco()).isEqualByComparingTo("40.00");
        assertThat(agendamento.getCustoInsumo()).isEqualByComparingTo("5.00");
        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
    }

    @Test
    void copiaEmMantemADuracaoEOsValoresDaEpoca() {
        Agendamento original = agendamento();
        original.getServico().setPreco(new BigDecimal("99.00"));

        Agendamento copia = original.copiaEm(INICIO.plusDays(1));

        assertThat(copia.duracao()).isEqualTo(Duration.ofMinutes(45));
        assertThat(copia.getPreco()).isEqualByComparingTo("40.00");
    }

    @Test
    void naoConcluiNemMarcaFaltaAntesDoHorario() {
        Agendamento agendamento = agendamento();

        assertThatThrownBy(() -> agendamento.concluir(INICIO.minusMinutes(1)))
                .isInstanceOf(TransicaoAgendamentoInvalidaException.class);
        assertThatThrownBy(() -> agendamento.marcarFalta(INICIO.minusMinutes(1)))
                .isInstanceOf(TransicaoAgendamentoInvalidaException.class);
    }

    @Test
    void concluiDepoisDoHorario() {
        Agendamento agendamento = agendamento();

        agendamento.concluir(INICIO);

        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CONCLUIDO);
    }

    @Test
    void statusFinalNaoMudaMais() {
        Agendamento agendamento = agendamento();
        agendamento.cancelar();

        assertThatThrownBy(agendamento::cancelar).isInstanceOf(TransicaoAgendamentoInvalidaException.class);
        assertThatThrownBy(() -> agendamento.concluir(INICIO.plusHours(1)))
                .isInstanceOf(TransicaoAgendamentoInvalidaException.class)
                .hasMessageContaining("cancelado");
        assertThatThrownBy(agendamento::liberarParaRemarcar)
                .isInstanceOf(TransicaoAgendamentoInvalidaException.class);
    }
}
