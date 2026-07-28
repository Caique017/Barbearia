package com.barbearia.api.dto.barbeiro;

import java.util.UUID;

public record BarbeiroPerfilResponse(
        UUID barbeiroId,
        String nome,
        String email,
        String horarioFuncionamento
) {
}
