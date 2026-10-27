package com.barbearia.api.dto.assinatura;

import com.barbearia.api.enums.FormaPagamento;
import com.barbearia.api.enums.StatusAssinatura;

import java.math.BigDecimal;
import java.time.Instant;

public record AssinaturaResponse(
        StatusAssinatura status,
        boolean ativa,
        Instant testeAte,
        Instant liberadaAte,
        long diasRestantes,
        FormaPagamento formaPagamento,
        BigDecimal precoMensal,
        boolean cancelamentoAgendado
) {
}
