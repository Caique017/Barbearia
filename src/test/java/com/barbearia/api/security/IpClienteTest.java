package com.barbearia.api.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class IpClienteTest {

    private static MockHttpServletRequest request(String encaminhado) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        if (encaminhado != null) {
            request.addHeader("X-Forwarded-For", encaminhado);
        }
        return request;
    }

    @Test
    void semProxyConfiavelIgnoraOHeader() {
        assertThat(new IpCliente(false).de(request("9.9.9.9"))).isEqualTo("10.0.0.5");
    }

    @Test
    void comProxyConfiavelUsaOUltimoSaltoQueEOQueOProxyViu() {
        assertThat(new IpCliente(true).de(request("6.6.6.6, 9.9.9.9"))).isEqualTo("9.9.9.9");
    }

    @Test
    void comProxyConfiavelESemHeaderUsaOEnderecoDaConexao() {
        assertThat(new IpCliente(true).de(request(null))).isEqualTo("10.0.0.5");
    }
}
