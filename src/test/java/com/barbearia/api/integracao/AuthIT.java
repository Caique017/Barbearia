package com.barbearia.api.integracao;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIT extends IntegracaoBase {

    private String cadastroBarbeiro(String email, String nomeBarbearia, String senha) {
        return json("nome", "Pessoa Teste", "nomeBarbearia", nomeBarbearia, "email", email, "senha", senha);
    }

    private String login(String email, String senha) {
        return json("email", email, "senha", senha);
    }

    @Test
    void cadastroDevolveTokenEOSlugDoNomeDaBarbearia() throws Exception {
        String sufixo = sufixoUnico();

        chamar("POST", "/auth/barbeiro/registro", null,
                cadastroBarbeiro(emailUnico(), "Barbearia " + sufixo, SENHA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.nome").value("Pessoa Teste"))
                .andExpect(jsonPath("$.slug").value("barbearia-" + sufixo));
    }

    @Test
    void emailRepetidoComOutraCaixaDa409() throws Exception {
        String sufixo = sufixoUnico();
        chamar("POST", "/auth/barbeiro/registro", null,
                cadastroBarbeiro("Fulano." + sufixo + "@Ex.COM", "Barbearia", SENHA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("fulano." + sufixo + "@ex.com"));

        chamar("POST", "/auth/barbeiro/registro", null,
                cadastroBarbeiro("fulano." + sufixo + "@ex.com", "Barbearia", SENHA))
                .andExpect(status().isConflict());
    }

    @Test
    void loginIgnoraCaixaDoEmail() throws Exception {
        String sufixo = sufixoUnico();
        chamar("POST", "/auth/barbeiro/registro", null,
                cadastroBarbeiro("Joao." + sufixo + "@Ex.com", "Barbearia", SENHA))
                .andExpect(status().isCreated());

        chamar("POST", "/auth/barbeiro/login", null, login(" joao." + sufixo + "@ex.com ", SENHA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void senhaErradaEContaDeOutroTipoDao401ComAMesmaMensagem() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();

        MvcResult senhaErrada = chamar("POST", "/auth/barbeiro/login", null, login(barbeiro.email(), "errada123"))
                .andExpect(status().isUnauthorized())
                .andReturn();
        MvcResult tipoErrado = chamar("POST", "/auth/cliente/login", null, login(barbeiro.email(), SENHA))
                .andExpect(status().isUnauthorized())
                .andReturn();
        MvcResult inexistente = chamar("POST", "/auth/barbeiro/login", null, login(emailUnico(), SENHA))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(ler(senhaErrada, "$.mensagem"))
                .isEqualTo(ler(tipoErrado, "$.mensagem"))
                .isEqualTo(ler(inexistente, "$.mensagem"));
    }

    @Test
    void cincoFalhasBloqueiamAteASenhaCerta() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();

        for (int i = 0; i < 5; i++) {
            chamar("POST", "/auth/barbeiro/login", null, login(barbeiro.email(), "errada123"))
                    .andExpect(status().isUnauthorized());
        }

        chamar("POST", "/auth/barbeiro/login", null, login(barbeiro.email(), SENHA))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
    }

    @Test
    void loginCertoZeraAsFalhas() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();

        for (int i = 0; i < 4; i++) {
            chamar("POST", "/auth/barbeiro/login", null, login(barbeiro.email(), "errada123"));
        }
        chamar("POST", "/auth/barbeiro/login", null, login(barbeiro.email(), SENHA)).andExpect(status().isOk());

        for (int i = 0; i < 4; i++) {
            chamar("POST", "/auth/barbeiro/login", null, login(barbeiro.email(), "errada123"));
        }
        chamar("POST", "/auth/barbeiro/login", null, login(barbeiro.email(), SENHA)).andExpect(status().isOk());
    }

    @Test
    void nomeBarbeariaSoComSimbolosDa400NoCampoCerto() throws Exception {
        chamar("POST", "/auth/barbeiro/registro", null, cadastroBarbeiro(emailUnico(), "!!! ###", SENHA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("nomeBarbearia"));
    }

    @Test
    void nomeEmAlfabetoNaoLatinoCaiNoSlugPadrao() throws Exception {
        MvcResult cadastro = chamar("POST", "/auth/barbeiro/registro", null,
                cadastroBarbeiro(emailUnico(), "理发店", SENHA))
                .andExpect(status().isCreated())
                .andReturn();

        assertThat(ler(cadastro, "$.slug")).matches("barbearia(-\\d+)?");
    }

    @Test
    void senhaCurtaOuAcimaDe72BytesDa400() throws Exception {
        chamar("POST", "/auth/barbeiro/registro", null, cadastroBarbeiro(emailUnico(), "Barbearia", "1234567"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("senha"));

        chamar("POST", "/auth/barbeiro/registro", null, cadastroBarbeiro(emailUnico(), "Barbearia", "ç".repeat(40)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("senha"));
    }
}
