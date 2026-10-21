package com.barbearia.api.compartilhado.erro;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResposta(
        Instant timestamp,
        int status,
        String erro,
        String mensagem,
        String caminho,
        List<CampoErro> campos
) {
    public record CampoErro(
            String campo,
            String mensagem
    ) {
    }
}
