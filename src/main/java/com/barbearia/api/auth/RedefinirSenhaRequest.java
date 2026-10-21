package com.barbearia.api.auth;

import com.barbearia.api.compartilhado.validacao.MaxBytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaRequest(
        @NotBlank
        @Size(max = 100)
        String token,
        @NotBlank
        @Size(min = 8, max = 64, message = "A senha deve ter entre 8 e 64 caracteres.")
        @MaxBytes(value = 72, message = "A senha é longa demais.")
        String novaSenha
) {
}
