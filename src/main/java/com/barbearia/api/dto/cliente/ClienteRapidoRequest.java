package com.barbearia.api.dto.cliente;

import com.barbearia.api.util.Telefone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteRapidoRequest(
        @NotBlank
        @Size(max = 100)
        String nome,
        @NotBlank
        @Pattern(regexp = Telefone.PADRAO, message = "Telefone inválido. Informe DDD + número.")
        String telefone
) {
    public ClienteRapidoRequest {
        telefone = Telefone.normalizar(telefone);
    }
}
