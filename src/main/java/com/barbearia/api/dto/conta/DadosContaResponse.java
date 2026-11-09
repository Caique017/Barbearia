package com.barbearia.api.dto.conta;

import com.barbearia.api.enums.FormaPagamento;
import com.barbearia.api.enums.Role;
import com.barbearia.api.enums.StatusAgendamento;
import com.barbearia.api.enums.StatusAssinatura;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record DadosContaResponse(
        Instant exportadoEm,
        Conta conta,
        Barbearia barbearia,
        CadastroCliente cadastroCliente
) {
    public record Conta(
            String nome,
            String email,
            Role tipo,
            Instant criadaEm,
            String termosVersao,
            Instant termosAceitosEm,
            Instant marketingAceitoEm,
            boolean recebeLembretes
    ) {
    }

    public record Barbearia(
            String nome,
            String slug,
            String horarioFuncionamento,
            Assinatura assinatura,
            List<Servico> servicos,
            List<Faixa> horarios,
            List<Bloqueio> bloqueios,
            List<Cliente> clientes,
            List<Atendimento> agendamentos
    ) {
    }

    public record Assinatura(
            StatusAssinatura status,
            FormaPagamento formaPagamento,
            Instant testeAte,
            Instant acessoAte
    ) {
    }

    public record Servico(
            String nome,
            BigDecimal preco,
            Integer duracaoMinutos,
            BigDecimal custoInsumo,
            boolean ativo
    ) {
    }

    public record Faixa(
            DayOfWeek diaSemana,
            LocalTime abre,
            LocalTime fecha
    ) {
    }

    public record Bloqueio(
            LocalDateTime inicio,
            LocalDateTime fim,
            String motivo
    ) {
    }

    public record Cliente(
            String nome,
            String telefone,
            boolean temConta
    ) {
    }

    public record CadastroCliente(
            String barbearia,
            String nome,
            String telefone,
            List<Atendimento> agendamentos
    ) {
    }

    public record Atendimento(
            LocalDateTime dataHora,
            StatusAgendamento status,
            String servico,
            String cliente,
            BigDecimal preco
    ) {
    }
}
