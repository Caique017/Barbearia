package com.barbearia.api.dto.senha;

import com.barbearia.api.util.Emails;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EsqueciSenhaRequest(
        @NotBlank
        @Size(max = 150)
        @Email
        String email
) {
    public EsqueciSenhaRequest {
        email = Emails.normalizar(email);
    }
}
