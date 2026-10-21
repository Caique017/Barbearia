package com.barbearia.api.integracao;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RedefinicaoSenhaIT extends IntegracaoBase {

    private static final String NOVA_SENHA = "novaSenha456";

    private void pedirLink(String email) throws Exception {
        chamar("POST", "/auth/senha/esqueci", null, json("email", email))
                .andExpect(status().isNoContent());
    }

    private String tokenDoLinkEnviado(String email) {
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(emailService, timeout(2000)).enviarRedefinicaoSenha(eq(email), anyString(), link.capture());
        String query = URI.create(link.getValue()).getQuery();
        return query.substring("token=".length());
    }

    private void login(String email, String senha, int statusEsperado) throws Exception {
        chamar("POST", "/auth/barbeiro/login", null, json("email", email, "senha", senha))
                .andExpect(status().is(statusEsperado));
    }

    @Test
    void fluxoCompletoTrocaASenha() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();

        pedirLink(barbeiro.email());
        String token = tokenDoLinkEnviado(barbeiro.email());

        chamar("POST", "/auth/senha/redefinir", null, json("token", token, "novaSenha", NOVA_SENHA))
                .andExpect(status().isNoContent());

        login(barbeiro.email(), NOVA_SENHA, 200);
        login(barbeiro.email(), SENHA, 401);
    }

    @Test
    void emailSemContaDa204ENaoEnvia() throws Exception {
        pedirLink(emailUnico());

        verify(emailService, never()).enviarRedefinicaoSenha(any(), any(), any());
    }

    @Test
    void pedirDeNovoEmSeguidaNaoEnviaOutro() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();

        pedirLink(barbeiro.email());
        pedirLink(barbeiro.email());

        verify(emailService, timeout(2000).times(1)).enviarRedefinicaoSenha(eq(barbeiro.email()), any(), any());
    }

    @Test
    void mesmoLinkDuasVezesDa400NaSegunda() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();
        pedirLink(barbeiro.email());
        String token = tokenDoLinkEnviado(barbeiro.email());
        String corpo = json("token", token, "novaSenha", NOVA_SENHA);

        chamar("POST", "/auth/senha/redefinir", null, corpo).andExpect(status().isNoContent());
        chamar("POST", "/auth/senha/redefinir", null, corpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Link inválido ou expirado. Peça um novo."));
    }

    @Test
    void tokenInventadoDa400() throws Exception {
        chamar("POST", "/auth/senha/redefinir", null, json("token", "inventado", "novaSenha", NOVA_SENHA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Link inválido ou expirado. Peça um novo."));
    }

    @Test
    void novaSenhaCurtaDa400DeValidacao() throws Exception {
        chamar("POST", "/auth/senha/redefinir", null, json("token", "qualquer", "novaSenha", "123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("novaSenha"));
    }

    @Test
    void redefinirLiberaOLoginBloqueadoPorTentativas() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();
        for (int i = 0; i < 5; i++) {
            login(barbeiro.email(), "errada123", 401);
        }
        login(barbeiro.email(), SENHA, 429);

        pedirLink(barbeiro.email());
        String token = tokenDoLinkEnviado(barbeiro.email());
        chamar("POST", "/auth/senha/redefinir", null, json("token", token, "novaSenha", NOVA_SENHA))
                .andExpect(status().isNoContent());

        login(barbeiro.email(), NOVA_SENHA, 200);
    }
}
