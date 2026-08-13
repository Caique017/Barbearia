package com.barbearia.api.dto.barbeiro;

import java.util.UUID;

public record BarbeiroPublicoResponse(
        UUID barbeiroId,
        String nome,
        String horarioFuncionamento
) {
}
