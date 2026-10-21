package com.barbearia.api.dto.financeiro;

import java.math.BigDecimal;

public record ResumoServico(
        String nome,
        Long atendimentos,
        BigDecimal faturamento
) {
}
