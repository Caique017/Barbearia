package com.barbearia.api.dto.agendamento;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record AgendamentoBarbeiroRequest(
        @NotNull
        UUID clienteId,
        @NotNull
        UUID servicoId,
        @NotNull
        LocalDateTime dataHora
) {
}
