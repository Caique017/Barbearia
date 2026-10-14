package com.barbearia.api.dto.cliente;

public record ClienteCadastroResponse(
        String token,
        String nome,
        String email,
        boolean vinculoPendente
) {
}
