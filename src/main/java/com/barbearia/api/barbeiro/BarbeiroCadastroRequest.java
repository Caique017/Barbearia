package com.barbearia.api.barbeiro;

import com.barbearia.api.compartilhado.util.Emails;
import com.barbearia.api.compartilhado.validacao.MaxBytes;
import jakarta.validation.constraints.*;

public record BarbeiroCadastroRequest(
        @NotBlank
        @Size(max = 100)
        String nome,
        @NotBlank
        @Size(max = 120)
        @Pattern(regexp = "(?s).*[\\p{L}\\p{N}].*",
                message = "O nome da barbearia deve ter pelo menos uma letra ou número.")
        String nomeBarbearia,
        @Size(min = 1, max = 255)
        @Pattern(regexp = "(?s).*\\S.*",
                message = "O horário de funcionamento não pode ficar em branco.")
        String horarioFuncionamento,
        @NotBlank
        @Size(max = 150)
        @Email
        String email,
        @NotBlank
        @Size(min = 8, max = 64, message = "A senha deve ter entre 8 e 64 caracteres.")
        @MaxBytes(value = 72, message = "A senha é longa demais.")
        String senha
) {
    public BarbeiroCadastroRequest {
        email = Emails.normalizar(email);
    }
}
