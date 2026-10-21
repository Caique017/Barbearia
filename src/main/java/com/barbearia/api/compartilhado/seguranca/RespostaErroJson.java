package com.barbearia.api.compartilhado.seguranca;

import com.barbearia.api.compartilhado.erro.ErroResposta;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class RespostaErroJson {

    private final ObjectMapper objectMapper;

    public void escrever(HttpServletRequest request, HttpServletResponse response,
                         HttpStatus status, String mensagem) throws IOException {
        ErroResposta corpo = new ErroResposta(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                mensagem,
                request.getRequestURI(),
                null
        );

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), corpo);
    }
}
