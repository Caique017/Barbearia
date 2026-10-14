package com.barbearia.api.dto.cliente;

public record ClienteLoginResponse(
        String nome,
        String email,
        String token
) {
}
