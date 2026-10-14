package com.barbearia.api.dto.barbeiro;

public record BarbeiroCadastroResponse(
        String token,
        String nome,
        String email,
        String slug
) {
}
