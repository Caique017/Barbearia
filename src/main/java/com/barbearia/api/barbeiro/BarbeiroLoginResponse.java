package com.barbearia.api.barbeiro;

public record BarbeiroLoginResponse(
        String token,
        String nome,
        String email
) {
}
