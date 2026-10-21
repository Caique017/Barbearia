package com.barbearia.api.cliente;

import com.barbearia.api.compartilhado.util.Emails;
import com.barbearia.api.compartilhado.validacao.MaxBytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClienteLoginRequest(
        @NotBlank
        @Size(max = 150)
        @Email
        String email,
        @NotBlank
        @Size(max = 64)
        @MaxBytes(72)
        String senha
) {
    public ClienteLoginRequest {
        email = Emails.normalizar(email);
    }
}
