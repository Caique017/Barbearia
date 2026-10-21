package com.barbearia.api.dto.assinatura;

import com.barbearia.api.enums.FormaPagamento;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotNull
        FormaPagamento formaPagamento
) {
}
