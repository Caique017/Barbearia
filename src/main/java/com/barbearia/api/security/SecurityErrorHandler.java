package com.barbearia.api.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final RespostaErroJson respostaErro;
    private final EventosSeguranca eventos;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        respostaErro.escrever(request, response, HttpStatus.UNAUTHORIZED,
                "Autenticação necessária. Token ausente ou inválido.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        eventos.acessoNegado(request.getMethod(), request.getRequestURI());
        respostaErro.escrever(request, response, HttpStatus.FORBIDDEN,
                "Acesso negado. Você não tem permissão para acessar este recurso.");
    }
}
