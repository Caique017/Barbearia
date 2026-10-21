package com.barbearia.api.dto.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ResumoFinanceiroResponse(
        LocalDate de,
        LocalDate ate,
        long atendimentos,
        BigDecimal faturamento,
        BigDecimal custos,
        BigDecimal lucro,
        List<ResumoDia> dias
) {
}
