package com.barbearia.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class OrigemConfiavelFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Origem-Segredo";
    private static final String HEALTH = "/actuator/health";

    private final byte[] segredo;
    private final RespostaErroJson respostaErro;
    private final EventosSeguranca eventos;

    public OrigemConfiavelFilter(@Value("${api.origem.segredo:}") String segredo, RespostaErroJson respostaErro,
                                 EventosSeguranca eventos) {
        this.segredo = segredo.getBytes(StandardCharsets.UTF_8);
        this.respostaErro = respostaErro;
        this.eventos = eventos;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (segredo.length == 0 || caminho(request).equals(HEALTH) || confere(request.getHeader(HEADER))) {
            filterChain.doFilter(request, response);
            return;
        }

        eventos.acessoNegado(request.getMethod(), request.getRequestURI());
        respostaErro.escrever(request, response, HttpStatus.FORBIDDEN, "Acesso negado.");
    }

    private boolean confere(String recebido) {
        return recebido != null && MessageDigest.isEqual(segredo, recebido.getBytes(StandardCharsets.UTF_8));
    }

    private static String caminho(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}
