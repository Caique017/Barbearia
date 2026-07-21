package com.barbearia.api.dto.auth;

public record BarbeiroCadastroResponse(
        String token,
        String nome,
        String email
) {
}
