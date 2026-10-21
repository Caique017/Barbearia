package com.barbearia.api.cliente;

import java.util.UUID;

public record ClienteResumoResponse(
        UUID id,
        String nome,
        String telefone,
        boolean temConta,
        PedidoVinculo pedidoVinculo
) {
    public record PedidoVinculo(
            String nome,
            String email
    ) {
    }
}
