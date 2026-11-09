package com.barbearia.api.dto.conta;

public record PreferenciasRequest(
        Boolean marketing,
        Boolean lembretes
) {
}
