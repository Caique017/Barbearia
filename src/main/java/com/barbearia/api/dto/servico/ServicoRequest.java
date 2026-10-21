package com.barbearia.api.dto.servico;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ServicoRequest(
        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "(?s).*[\\p{L}\\p{N}].*",
                message = "O nome deve ter pelo menos uma letra ou número.")
        String nome,
        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 8, fraction = 2)
        BigDecimal preco,
        @NotNull
        @Min(5)
        @Max(480)
        Integer duracaoMinutos,
        @DecimalMin("0.00")
        @Digits(integer = 8, fraction = 2)
        BigDecimal custoInsumo
) {
    public ServicoRequest {
        nome = nome == null ? null : nome.trim();
        custoInsumo = custoInsumo == null ? BigDecimal.ZERO : custoInsumo;
    }
}
