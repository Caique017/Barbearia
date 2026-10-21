package com.barbearia.api.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IpCliente {

    private final boolean atrasDeProxyConfiavel;

    public IpCliente(@Value("${api.proxy.confiavel:false}") boolean atrasDeProxyConfiavel) {
        this.atrasDeProxyConfiavel = atrasDeProxyConfiavel;
    }

    public String de(HttpServletRequest request) {
        if (atrasDeProxyConfiavel) {
            String encaminhado = request.getHeader("X-Forwarded-For");
            if (encaminhado != null && !encaminhado.isBlank()) {
                String[] saltos = encaminhado.split(",");
                return saltos[saltos.length - 1].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
