package com.barbearia.api.dto.agendamento;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record HorariosRequest(
        @NotNull
        @Size(max = 28)
        List<@Valid @NotNull FaixaHorario> faixas
) {
}
