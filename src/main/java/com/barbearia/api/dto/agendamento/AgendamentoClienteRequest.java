package com.barbearia.api.dto.agendamento;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record AgendamentoClienteRequest(
        @NotNull
        UUID servicoId,
        @NotNull
        LocalDateTime dataHora
) {
}
