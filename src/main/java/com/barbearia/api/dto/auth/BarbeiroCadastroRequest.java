package com.barbearia.api.dto.auth;

import jakarta.validation.constraints.*;

public record BarbeiroCadastroRequest(
        @NotBlank
        @Size(max = 100)
        String nome,
        @NotBlank
        @Size(max = 150)
        @Email
        String email,
        @NotBlank
        @Size(min = 6, max = 15)
        String senha
) {
}
