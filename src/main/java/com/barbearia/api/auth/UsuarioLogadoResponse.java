package com.barbearia.api.auth;

import java.util.UUID;

public record UsuarioLogadoResponse(
        UUID id,
        String nome,
        String email,
        Role role
) {
}
