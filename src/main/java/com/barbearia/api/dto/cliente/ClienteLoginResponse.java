package com.barbearia.api.dto.cliente;

public record ClienteLoginResponse(
        String token,
        String nome,
        String email
) {
}
