package com.barbearia.api.integracao;

import com.barbearia.api.TestcontainersConfiguration;
import com.barbearia.api.auth.EmailService;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
abstract class IntegracaoBase {

    protected static final String SENHA = "senha123";

    @Autowired
    protected MockMvc mvc;

    @MockitoBean
    protected EmailService emailService;

    protected record BarbeiroCriado(String token, String email, String slug, String barbeiroId) {
    }

    protected static String emailUnico() {
        return "it." + UUID.randomUUID() + "@ex.com";
    }

    protected static String telefoneUnico() {
        return "119" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    }

    protected static String sufixoUnico() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    protected ResultActions chamar(String metodo, String caminho, String token, String corpo) throws Exception {
        MockHttpServletRequestBuilder requisicao = request(HttpMethod.valueOf(metodo), caminho);
        if (token != null) {
            requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (corpo != null) {
            requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo);
        }
        return mvc.perform(requisicao);
    }

    protected BarbeiroCriado registrarBarbeiro(String nomeBarbearia) throws Exception {
        String email = emailUnico();
        MvcResult cadastro = chamar("POST", "/auth/barbeiro/registro", null, json(
                "nome", "Pessoa Teste",
                "nomeBarbearia", nomeBarbearia,
                "email", email,
                "senha", SENHA))
                .andExpect(status().isCreated())
                .andReturn();

        String slug = ler(cadastro, "$.slug");
        MvcResult publico = chamar("GET", "/barbeiros/publico/" + slug, null, null)
                .andExpect(status().isOk())
                .andReturn();

        return new BarbeiroCriado(ler(cadastro, "$.token"), email, slug, ler(publico, "$.barbeiroId"));
    }

    protected BarbeiroCriado registrarBarbeiro() throws Exception {
        return registrarBarbeiro("Barbearia " + sufixoUnico());
    }

    protected ResultActions registrarCliente(String barbeiroId, String nome, String telefone, String email)
            throws Exception {
        return chamar("POST", "/auth/cliente/registro", null, json(
                "nome", nome,
                "telefone", telefone,
                "email", email,
                "senha", SENHA,
                "barbeiroId", barbeiroId));
    }

    protected String cadastrarClientePeloBarbeiro(String tokenBarbeiro, String nome, String telefone)
            throws Exception {
        MvcResult resultado = chamar("POST", "/clientes", tokenBarbeiro, json("nome", nome, "telefone", telefone))
                .andExpect(status().isCreated())
                .andReturn();
        return ler(resultado, "$.id");
    }

    protected static String ler(MvcResult resultado, String caminho) throws Exception {
        Object valor = JsonPath.read(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8), caminho);
        return valor == null ? null : valor.toString();
    }

    protected static String json(String... chavesEValores) {
        StringJoiner objeto = new StringJoiner(",", "{", "}");
        for (int i = 0; i < chavesEValores.length; i += 2) {
            String valor = chavesEValores[i + 1];
            objeto.add(texto(chavesEValores[i]) + ":" + (valor == null ? "null" : texto(valor)));
        }
        return objeto.toString();
    }

    private static String texto(String valor) {
        return "\"" + valor.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
