package com.barbearia.api.barbeiro;

import java.util.UUID;

public record BarbeiroPerfilResponse(
        UUID barbeiroId,
        String nome,
        String email,
        String nomeBarbearia,
        String slug,
        String horarioFuncionamento
) {
}
