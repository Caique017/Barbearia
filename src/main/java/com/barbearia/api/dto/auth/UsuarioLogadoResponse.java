package com.barbearia.api.dto.auth;

import com.barbearia.api.enums.Role;

import java.util.UUID;

public record UsuarioLogadoResponse(
        UUID id,
        String nome,
        String email,
        Role role
) {
}
