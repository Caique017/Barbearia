package com.barbearia.api.dto.agendamento;

import com.barbearia.api.enums.StatusAgendamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AgendamentoResponse(
        UUID id,
        LocalDateTime dataHora,
        LocalDateTime terminaEm,
        StatusAgendamento status,
        BigDecimal preco,
        Servico servico,
        Cliente cliente
) {
    public record Servico(
            UUID id,
            String nome
    ) {
    }

    public record Cliente(
            UUID id,
            String nome,
            String telefone
    ) {
    }
}
