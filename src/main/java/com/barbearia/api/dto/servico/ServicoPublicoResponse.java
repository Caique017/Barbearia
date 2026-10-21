package com.barbearia.api.dto.servico;

import java.math.BigDecimal;
import java.util.UUID;

public record ServicoPublicoResponse(
        UUID id,
        String nome,
        BigDecimal preco,
        Integer duracaoMinutos
) {
}
