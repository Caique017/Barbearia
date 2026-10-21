package com.barbearia.api.dto.agendamento;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record BloqueioRequest(
        @NotNull
        LocalDateTime inicio,
        @NotNull
        LocalDateTime fim,
        @Size(max = 120)
        String motivo
) {
    public BloqueioRequest {
        motivo = motivo == null || motivo.isBlank() ? null : motivo.trim();
    }
}
