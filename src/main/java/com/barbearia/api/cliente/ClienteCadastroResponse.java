package com.barbearia.api.cliente;

public record ClienteCadastroResponse(
        String token,
        String nome,
        String email,
        boolean vinculoPendente
) {
}
