package com.barbearia.api.integracao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SegurancaIT extends IntegracaoBase {

    private static final String ID = UUID.randomUUID().toString();

    static Stream<Arguments> rotasDeBarbeiro() {
        return Stream.of(
                Arguments.of("GET", "/barbeiros/me", null),
                Arguments.of("PATCH", "/barbeiros/me", "{}"),
                Arguments.of("PATCH", "/barbeiros/me/slug", "{\"slug\":\"qualquer-um\"}"),
                Arguments.of("GET", "/clientes", null),
                Arguments.of("POST", "/clientes", "{\"nome\":\"X\",\"telefone\":\"11999998888\"}"),
                Arguments.of("GET", "/clientes/" + ID, null),
                Arguments.of("PATCH", "/clientes/" + ID, "{}"),
                Arguments.of("POST", "/clientes/" + ID + "/vinculo", null),
                Arguments.of("DELETE", "/clientes/" + ID + "/vinculo", null),
                Arguments.of("GET", "/servicos", null),
                Arguments.of("POST", "/servicos", "{\"nome\":\"X\",\"preco\":1,\"duracaoMinutos\":10}"),
                Arguments.of("GET", "/servicos/" + ID, null),
                Arguments.of("PATCH", "/servicos/" + ID, "{}"));
    }

    static Stream<Arguments> rotasProtegidas() {
        return Stream.concat(Stream.of(
                Arguments.of("GET", "/auth/me", null),
                Arguments.of("POST", "/auth/sair", null)), rotasDeBarbeiro());
    }

    @ParameterizedTest(name = "{0} {1} sem token -> 401")
    @MethodSource("rotasProtegidas")
    void semTokenDa401(String metodo, String caminho, String corpo) throws Exception {
        chamar(metodo, caminho, null, corpo).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0} {1} com token invalido -> 401")
    @MethodSource("rotasProtegidas")
    void tokenInvalidoDa401(String metodo, String caminho, String corpo) throws Exception {
        chamar(metodo, caminho, "lixo", corpo).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0} {1} com token de CLIENTE -> 403")
    @MethodSource("rotasDeBarbeiro")
    void tokenDeClienteDa403(String metodo, String caminho, String corpo) throws Exception {
        chamar(metodo, caminho, tokenDeCliente(), corpo).andExpect(status().isForbidden());
    }

    @Test
    void clienteConsegueVerOProprioAuthMe() throws Exception {
        chamar("GET", "/auth/me", tokenDeCliente(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CLIENTE"));
    }

    @Test
    void rotaPublicaNaoPedeTokenENaoExpoeEmail() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();

        chamar("GET", "/barbeiros/publico/" + barbeiro.slug(), null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.barbeiroId").value(barbeiro.barbeiroId()))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.slug").doesNotExist());
    }

    @Test
    void tokenRuimNaoDerrubaRotaPublica() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();

        chamar("GET", "/barbeiros/publico/" + barbeiro.slug(), "lixo", null).andExpect(status().isOk());
    }

    @Test
    void rotaPublicaComSlugInexistenteDa404() throws Exception {
        chamar("GET", "/barbeiros/publico/nao-existe-" + UUID.randomUUID(), null, null)
                .andExpect(status().isNotFound());
    }

    @Test
    void respostasLevamOsHeadersDeSeguranca() throws Exception {
        chamar("GET", "/auth/me", null, null)
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void metodoErradoDa405EnaoErroInterno() throws Exception {
        chamar("PUT", "/auth/me", tokenDeCliente(), "{}").andExpect(status().isMethodNotAllowed());
    }

    @Test
    void corpoQueNaoEJsonDa415EnaoErroInterno() throws Exception {
        mvc.perform(post("/auth/cliente/login").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType());
    }

    private String tokenDeCliente() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();
        MvcResult cadastro = registrarCliente(barbeiro.barbeiroId(), "Cliente", telefoneUnico(), emailUnico())
                .andExpect(status().isCreated())
                .andReturn();
        return ler(cadastro, "$.token");
    }
}
