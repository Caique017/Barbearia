package com.barbearia.api.dto.cliente;

import com.barbearia.api.util.Emails;
import com.barbearia.api.util.Telefone;
import com.barbearia.api.validation.MaxBytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ClienteCadastroRequest(
        @NotBlank
        @Size(max = 100)
        String nome,
        @NotBlank
        @Pattern(regexp = Telefone.PADRAO, message = "Telefone inválido. Informe DDD + número.")
        String telefone,
        @NotBlank
        @Size(max = 150)
        @Email
        String email,
        @NotBlank
        @Size(min = 8, max = 64, message = "A senha deve ter entre 8 e 64 caracteres.")
        @MaxBytes(value = 72, message = "A senha é longa demais.")
        String senha,
        @NotNull
        UUID barbeiroId
) {
    public ClienteCadastroRequest {
        telefone = Telefone.normalizar(telefone);
        email = Emails.normalizar(email);
    }
}
