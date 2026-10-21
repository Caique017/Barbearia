package com.barbearia.api.dto.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResumoDia(
        LocalDate data,
        Long atendimentos,
        BigDecimal faturamento,
        BigDecimal custos,
        BigDecimal lucro
) {
}
