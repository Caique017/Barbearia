package com.barbearia.api.dto.barbeiro;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BarbeiroAtualizacaoRequest(
        @Size(min = 1, max = 120)
        @Pattern(regexp = "(?s).*[\\p{L}\\p{N}].*",
                message = "O nome da barbearia deve ter pelo menos uma letra ou número.")
        String nomeBarbearia,
        @Size(min = 1, max = 255)
        @Pattern(regexp = "(?s).*\\S.*",
                message = "O horário de funcionamento não pode ficar em branco.")
        String horarioFuncionamento
) {
}
