package com.barbearia.api.compartilhado.seguranca;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Component
public class LimiteRequisicoesPorIpFilter extends OncePerRequestFilter {

    static final String MENSAGEM = "Muitas requisições. Tente novamente em alguns minutos.";
    private static final int LIMITE_PARA_LIMPEZA = 10_000;

    private static final Set<String> LOGIN = Set.of("/auth/barbeiro/login", "/auth/cliente/login");
    private static final Set<String> CADASTRO = Set.of("/auth/barbeiro/registro", "/auth/cliente/registro");
    private static final Set<String> SENHA = Set.of("/auth/senha/esqueci", "/auth/senha/redefinir");

    private final LimitesPorIp limites;
    private final IpCliente ipCliente;
    private final RespostaErroJson respostaErro;
    private final EventosSeguranca eventos;
    private final Clock clock;
    private final Map<String, Janela> janelas = new ConcurrentHashMap<>();

    public LimiteRequisicoesPorIpFilter(LimitesPorIp limites, IpCliente ipCliente, RespostaErroJson respostaErro,
                                        EventosSeguranca eventos, Clock clock) {
        this.limites = limites;
        this.ipCliente = ipCliente;
        this.respostaErro = respostaErro;
        this.eventos = eventos;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String nomeRegra = regraDe(request);
        if (nomeRegra == null) {
            filterChain.doFilter(request, response);
            return;
        }

        LimitesPorIp.Regra regra = regra(nomeRegra);
        String ip = ipCliente.de(request);
        Instant agora = clock.instant();
        Janela janela = janelas.compute(nomeRegra + ":" + ip, (chave, atual) ->
                atual == null || atual.expirada(agora, regra.janela())
                        ? new Janela(1, agora)
                        : new Janela(atual.quantidade() + 1, atual.inicio()));

        limparSeCheio(agora);

        if (janela.quantidade() > regra.maximo()) {
            long segundos = Math.max(1, Duration.between(agora, janela.inicio().plus(regra.janela())).toSeconds());
            eventos.limiteDeRequisicoesExcedido(nomeRegra, ip);
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(segundos));
            respostaErro.escrever(request, response, HttpStatus.TOO_MANY_REQUESTS, MENSAGEM);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String regraDe(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return null;
        }
        String caminho = request.getRequestURI().substring(request.getContextPath().length());
        if (LOGIN.contains(caminho)) {
            return "login";
        }
        if (CADASTRO.contains(caminho)) {
            return "cadastro";
        }
        if (SENHA.contains(caminho)) {
            return "senha";
        }
        return null;
    }

    private LimitesPorIp.Regra regra(String nome) {
        return switch (nome) {
            case "login" -> limites.login();
            case "cadastro" -> limites.cadastro();
            default -> limites.senha();
        };
    }

    private void limparSeCheio(Instant agora) {
        if (janelas.size() > LIMITE_PARA_LIMPEZA) {
            Duration maiorJanela = Stream.of(limites.login(), limites.cadastro(), limites.senha())
                    .map(LimitesPorIp.Regra::janela)
                    .max(Duration::compareTo)
                    .orElseThrow();
            janelas.values().removeIf(janela -> janela.expirada(agora, maiorJanela));
        }
    }

    private record Janela(int quantidade, Instant inicio) {

        boolean expirada(Instant agora, Duration duracao) {
            return !agora.isBefore(inicio.plus(duracao));
        }
    }
}
