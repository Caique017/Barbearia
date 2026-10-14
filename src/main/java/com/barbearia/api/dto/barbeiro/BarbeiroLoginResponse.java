package com.barbearia.api.dto.barbeiro;

public record BarbeiroLoginResponse(
        String nome,
        String email,
        String token
) {
}
