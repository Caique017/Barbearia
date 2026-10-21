package com.barbearia.api.dto.servico;

import java.math.BigDecimal;
import java.util.UUID;

public record ServicoResponse(
        UUID id,
        String nome,
        BigDecimal preco,
        Integer duracaoMinutos,
        BigDecimal custoInsumo,
        boolean ativo
) {
}
