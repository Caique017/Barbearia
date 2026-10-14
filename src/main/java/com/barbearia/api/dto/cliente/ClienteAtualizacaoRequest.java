package com.barbearia.api.dto.cliente;

import com.barbearia.api.util.Telefone;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteAtualizacaoRequest(
        @Size(min = 1, max = 100)
        @Pattern(regexp = "(?s).*[\\p{L}\\p{N}].*",
                message = "O nome deve ter pelo menos uma letra ou número.")
        String nome,
        @Pattern(regexp = Telefone.PADRAO, message = "Telefone inválido. Informe DDD + número.")
        String telefone
) {
    public ClienteAtualizacaoRequest {
        telefone = Telefone.normalizar(telefone);
    }
}
