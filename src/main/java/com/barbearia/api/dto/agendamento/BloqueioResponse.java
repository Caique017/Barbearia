package com.barbearia.api.dto.agendamento;

import java.time.LocalDateTime;
import java.util.UUID;

public record BloqueioResponse(
        UUID id,
        LocalDateTime inicio,
        LocalDateTime fim,
        String motivo,
        int agendamentosNoPeriodo
) {
}
