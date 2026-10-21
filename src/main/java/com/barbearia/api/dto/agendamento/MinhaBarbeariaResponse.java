package com.barbearia.api.dto.agendamento;

public record MinhaBarbeariaResponse(
        String slug,
        String nome,
        String horarioFuncionamento
) {
}
