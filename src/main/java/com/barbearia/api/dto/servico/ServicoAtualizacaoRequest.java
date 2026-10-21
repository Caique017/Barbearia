package com.barbearia.api.dto.servico;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ServicoAtualizacaoRequest(
        @Size(min = 1, max = 100)
        @Pattern(regexp = "(?s).*[\\p{L}\\p{N}].*",
                message = "O nome deve ter pelo menos uma letra ou número.")
        String nome,
        @DecimalMin("0.00")
        @Digits(integer = 8, fraction = 2)
        BigDecimal preco,
        @Min(5)
        @Max(480)
        Integer duracaoMinutos,
        @DecimalMin("0.00")
        @Digits(integer = 8, fraction = 2)
        BigDecimal custoInsumo,
        Boolean ativo
) {
    public ServicoAtualizacaoRequest {
        nome = nome == null ? null : nome.trim();
    }
}
