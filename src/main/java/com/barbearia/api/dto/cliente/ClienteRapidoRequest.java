package com.barbearia.api.dto.cliente;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClienteRapidoRequest(
        @NotBlank
        @Size(max = 100)
        String nome,
        @NotBlank
        @Size(max = 20)
        String telefone
) {
}