package com.barbearia.api.dto.auth;

public record ClienteLoginResponse(
        String nome,
        String email,
        String token
) {
}
