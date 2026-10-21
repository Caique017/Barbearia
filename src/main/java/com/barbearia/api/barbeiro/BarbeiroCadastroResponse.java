package com.barbearia.api.barbeiro;

public record BarbeiroCadastroResponse(
        String token,
        String nome,
        String email,
        String slug
) {
}
