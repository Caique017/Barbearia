package com.barbearia.api.dto.auth;

public record ClienteCadastroResponse(
        String token,
        String nome,
        String email
) {
}
