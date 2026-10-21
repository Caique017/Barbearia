package com.barbearia.api.dto.agendamento;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record DisponibilidadeResponse(
        LocalDate data,
        UUID servicoId,
        List<LocalTime> horarios
) {
}
