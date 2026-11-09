package com.barbearia.api.dto.conta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DescadastroRequest(
        @NotBlank
        @Size(max = 500)
        String token
) {
}
