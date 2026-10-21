package com.barbearia.api.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class OrigemConfiavelFilterTest {

    private final FilterChain cadeia = mock(FilterChain.class);

    private OrigemConfiavelFilter filtro(String segredo) {
        return new OrigemConfiavelFilter(segredo, new RespostaErroJson(JsonMapper.builder().build()),
                mock(EventosSeguranca.class));
    }

    private static MockHttpServletRequest request(String caminho, String segredo) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", caminho);
        if (segredo != null) {
            request.addHeader(OrigemConfiavelFilter.HEADER, segredo);
        }
        return request;
    }

    @Test
    void semSegredoConfiguradoDeixaTudoPassar() throws Exception {
        MockHttpServletRequest request = request("/auth/me", null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro("").doFilter(request, response, cadeia);

        verify(cadeia).doFilter(request, response);
    }

    @Test
    void segredoCertoPassa() throws Exception {
        MockHttpServletRequest request = request("/auth/me", "abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro("abc123").doFilter(request, response, cadeia);

        verify(cadeia).doFilter(request, response);
    }

    @Test
    void segredoErradoOuAusenteDa403() throws Exception {
        for (String recebido : new String[]{null, "errado"}) {
            MockHttpServletResponse response = new MockHttpServletResponse();

            filtro("abc123").doFilter(request("/auth/me", recebido), response, cadeia);

            assertThat(response.getStatus()).isEqualTo(403);
        }
        verify(cadeia, never()).doFilter(any(), any());
    }

    @Test
    void healthCheckNaoPrecisaDoSegredo() throws Exception {
        MockHttpServletRequest request = request("/actuator/health", null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro("abc123").doFilter(request, response, cadeia);

        verify(cadeia).doFilter(request, response);
    }
}
