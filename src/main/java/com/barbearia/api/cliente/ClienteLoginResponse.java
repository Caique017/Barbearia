package com.barbearia.api.cliente;

public record ClienteLoginResponse(
        String token,
        String nome,
        String email
) {
}
