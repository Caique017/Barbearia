package com.barbearia.api.dto.conta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExclusaoContaRequest(
        @NotBlank(message = "Informe a sua senha para confirmar.")
        @Size(max = 64)
        String senha
) {
}
