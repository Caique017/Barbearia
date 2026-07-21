package com.barbearia.api.dto.barbeiro;

import java.util.UUID;

public record BarbeiroPerfilResponse(
        UUID id,
        String nome,
        String email,
        String horarioFuncionamento
) {
}
