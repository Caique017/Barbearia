package com.barbearia.api.dto.barbeiro;

public record BarbeiroLoginResponse(
        String token,
        String nome,
        String email
) {
}
