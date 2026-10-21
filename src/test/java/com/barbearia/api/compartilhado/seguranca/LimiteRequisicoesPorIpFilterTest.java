package com.barbearia.api.compartilhado.seguranca;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LimiteRequisicoesPorIpFilterTest {

    private static final Instant INICIO = Instant.parse("2026-10-20T12:00:00Z");

    private final EventosSeguranca eventos = mock(EventosSeguranca.class);
    private RelogioAjustavel relogio;
    private LimiteRequisicoesPorIpFilter filtro;

    @BeforeEach
    void criarFiltro() {
        relogio = new RelogioAjustavel(INICIO);
        LimitesPorIp limites = new LimitesPorIp(
                new LimitesPorIp.Regra(2, Duration.ofMinutes(1)),
                new LimitesPorIp.Regra(1, Duration.ofHours(1)),
                new LimitesPorIp.Regra(1, Duration.ofMinutes(15)));
        filtro = new LimiteRequisicoesPorIpFilter(limites, new IpCliente(false),
                new RespostaErroJson(JsonMapper.builder().build()), eventos, relogio);
    }

    private MockHttpServletResponse chamar(String metodo, String caminho, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(metodo, caminho);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filtro.doFilter(request, response, mock(FilterChain.class));
        return response;
    }

    @Test
    void passaDoLimiteDa429ComRetryAfter() throws Exception {
        assertThat(chamar("POST", "/auth/barbeiro/login", "1.1.1.1").getStatus()).isEqualTo(200);
        assertThat(chamar("POST", "/auth/cliente/login", "1.1.1.1").getStatus()).isEqualTo(200);

        MockHttpServletResponse bloqueada = chamar("POST", "/auth/barbeiro/login", "1.1.1.1");

        assertThat(bloqueada.getStatus()).isEqualTo(429);
        assertThat(bloqueada.getHeader("Retry-After")).isEqualTo("60");
        assertThat(bloqueada.getContentAsString()).contains(LimiteRequisicoesPorIpFilter.MENSAGEM);
        verify(eventos).limiteDeRequisicoesExcedido("login", "1.1.1.1");
    }

    @Test
    void ipsDiferentesTemContagensSeparadas() throws Exception {
        chamar("POST", "/auth/barbeiro/registro", "1.1.1.1");

        assertThat(chamar("POST", "/auth/barbeiro/registro", "2.2.2.2").getStatus()).isEqualTo(200);
        assertThat(chamar("POST", "/auth/cliente/registro", "1.1.1.1").getStatus()).isEqualTo(429);
    }

    @Test
    void regrasDiferentesTemContagensSeparadas() throws Exception {
        chamar("POST", "/auth/senha/esqueci", "1.1.1.1");

        assertThat(chamar("POST", "/auth/barbeiro/login", "1.1.1.1").getStatus()).isEqualTo(200);
        assertThat(chamar("POST", "/auth/senha/redefinir", "1.1.1.1").getStatus()).isEqualTo(429);
    }

    @Test
    void depoisDaJanelaLiberaDeNovo() throws Exception {
        chamar("POST", "/auth/senha/esqueci", "1.1.1.1");
        assertThat(chamar("POST", "/auth/senha/esqueci", "1.1.1.1").getStatus()).isEqualTo(429);

        relogio.avancar(Duration.ofMinutes(15));

        assertThat(chamar("POST", "/auth/senha/esqueci", "1.1.1.1").getStatus()).isEqualTo(200);
    }

    @Test
    void rotasForaDasRegrasNaoContam() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertThat(chamar("GET", "/auth/me", "1.1.1.1").getStatus()).isEqualTo(200);
            assertThat(chamar("GET", "/auth/barbeiro/login", "1.1.1.1").getStatus()).isEqualTo(200);
        }
    }

    private static final class RelogioAjustavel extends Clock {

        private Instant agora;

        RelogioAjustavel(Instant agora) {
            this.agora = agora;
        }

        void avancar(Duration duracao) {
            agora = agora.plus(duracao);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }
}
