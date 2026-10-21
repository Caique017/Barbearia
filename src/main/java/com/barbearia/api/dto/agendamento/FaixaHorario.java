package com.barbearia.api.dto.agendamento;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record FaixaHorario(
        @NotNull
        DayOfWeek diaSemana,
        @NotNull
        LocalTime abre,
        @NotNull
        LocalTime fecha
) {
}
