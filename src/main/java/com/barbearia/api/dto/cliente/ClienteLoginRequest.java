package com.barbearia.api.dto.cliente;

import com.barbearia.api.util.Emails;
import com.barbearia.api.validation.MaxBytes;
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
