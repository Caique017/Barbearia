package com.barbearia.api.config;

import com.barbearia.api.domain.Usuario;
import com.barbearia.api.enums.Role;
import com.barbearia.api.service.AssinaturaService;
import com.barbearia.api.service.BarbeiroLogado;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AssinaturaInterceptor implements HandlerInterceptor {

    private static final Set<String> METODOS_DE_LEITURA = Set.of("GET", "HEAD", "OPTIONS");
    private static final List<String> SEMPRE_LIBERADOS = List.of("/auth/", "/assinatura", "/pagamentos/");

    private final AssinaturaService assinaturaService;
    private final BarbeiroLogado barbeiroLogado;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        if (METODOS_DE_LEITURA.contains(request.getMethod())) {
            return true;
        }

        String caminho = request.getRequestURI().substring(request.getContextPath().length());
        if (SEMPRE_LIBERADOS.stream().anyMatch(caminho::startsWith)) {
            return true;
        }

        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.getPrincipal() instanceof Usuario usuario
                && usuario.getRole() == Role.BARBEIRO) {
            assinaturaService.exigirAtiva(barbeiroLogado.de(usuario));
        }
        return true;
    }
}
