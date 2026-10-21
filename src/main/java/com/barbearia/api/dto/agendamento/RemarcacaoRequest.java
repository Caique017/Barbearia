package com.barbearia.api.dto.agendamento;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RemarcacaoRequest(
        @NotNull
        LocalDateTime dataHora
) {
}
