package com.barbearia.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ClienteCadastroRequest(
        @NotBlank
        @Size(max = 100)
        String nome,
        @NotBlank
        @Size(max = 20)
        String telefone,
        @NotBlank
        @Size(max = 150)
        @Email
        String email,
        @NotBlank
        @Size(min = 6, max = 15)
        String senha,
        @NotNull
        UUID barbeiroId
) {
}