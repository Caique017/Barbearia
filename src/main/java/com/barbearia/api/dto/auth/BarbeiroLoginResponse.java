package com.barbearia.api.dto.auth;

public record BarbeiroLoginResponse(
        String nome,
        String email,
        String token
) {
}
