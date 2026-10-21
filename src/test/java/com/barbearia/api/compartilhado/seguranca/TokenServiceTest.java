package com.barbearia.api.compartilhado.seguranca;

import com.barbearia.api.auth.Role;
import com.barbearia.api.auth.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-pelo-menos-32-bytes";

    private static TokenService tokenService(String segredo, long horas) {
        TokenService service = new TokenService();
        ReflectionTestUtils.setField(service, "secret", segredo);
        ReflectionTestUtils.setField(service, "expirationHours", horas);
        return service;
    }

    private static final Usuario USUARIO = Usuario.builder().email("a@ex.com").role(Role.BARBEIRO).build();

    @Test
    void tokenGeradoValidaEDevolveOEmail() {
        TokenService service = tokenService(SEGREDO, 2);

        assertThat(service.validarToken(service.gerarToken(USUARIO)).email()).isEqualTo("a@ex.com");
    }

    @Test
    void tokenAssinadoComOutroSegredoNaoValida() {
        String token = tokenService("outro-segredo-tambem-com-mais-de-32-bytes", 2).gerarToken(USUARIO);

        assertThat(tokenService(SEGREDO, 2).validarToken(token)).isNull();
    }

    @Test
    void tokenAdulteradoNaoValida() {
        TokenService service = tokenService(SEGREDO, 2);
        String token = service.gerarToken(USUARIO);
        int inicioAssinatura = token.lastIndexOf('.') + 1;
        char original = token.charAt(inicioAssinatura);
        String adulterado = token.substring(0, inicioAssinatura) + (original == 'A' ? 'B' : 'A')
                + token.substring(inicioAssinatura + 1);

        assertThat(service.validarToken(adulterado)).isNull();
    }

    @Test
    void tokenVencidoNaoValida() {
        TokenService service = tokenService(SEGREDO, -1);

        assertThat(service.validarToken(service.gerarToken(USUARIO))).isNull();
    }

    @Test
    void tokenCarregaAVersaoDeSessaoDoUsuario() {
        TokenService service = tokenService(SEGREDO, 2);
        Usuario usuario = Usuario.builder().email("a@ex.com").role(Role.BARBEIRO).versaoSessao(3).build();

        assertThat(service.validarToken(service.gerarToken(usuario)).versaoSessao()).isEqualTo(3);
    }

    @Test
    void lixoNaoValida() {
        assertThat(tokenService(SEGREDO, 2).validarToken("lixo")).isNull();
    }

    @Test
    void segredoCurtoImpedeASubida() {
        assertThatThrownBy(() -> tokenService("curto", 2).validarSegredo())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void segredoAusenteImpedeASubida() {
        assertThatThrownBy(() -> tokenService(null, 2).validarSegredo())
                .isInstanceOf(IllegalStateException.class);
    }
}
