package com.barbearia.api.dto.cliente;

import java.util.UUID;

public record ClienteResumoResponse(
        UUID id,
        String nome,
        String telefone,
        boolean temConta
) {
}