package com.barbearia.api.integracao;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServicoIT extends IntegracaoBase {

    private String criarServico(String token, String nome, String preco, String duracao) throws Exception {
        MvcResult resultado = chamar("POST", "/servicos", token,
                "{\"nome\":\"" + nome + "\",\"preco\":" + preco + ",\"duracaoMinutos\":" + duracao + "}")
                .andExpect(status().isCreated())
                .andReturn();
        return ler(resultado, "$.id");
    }

    @Test
    void barbeiroCriaListaEEditaOsProprios() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();
        String id = criarServico(barbeiro.token(), "Corte", "40.00", "30");

        chamar("PATCH", "/servicos/" + id, barbeiro.token(), "{\"preco\":45.50}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preco").value(45.50))
                .andExpect(jsonPath("$.nome").value("Corte"));

        chamar("GET", "/servicos", barbeiro.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].custoInsumo").value(0));
    }

    @Test
    void servicoDeOutroBarbeiroDa404() throws Exception {
        BarbeiroCriado a = registrarBarbeiro();
        BarbeiroCriado b = registrarBarbeiro();
        String doA = criarServico(a.token(), "Corte", "40.00", "30");

        chamar("GET", "/servicos/" + doA, b.token(), null).andExpect(status().isNotFound());
        chamar("PATCH", "/servicos/" + doA, b.token(), "{\"preco\":1}").andExpect(status().isNotFound());
        chamar("GET", "/servicos", b.token(), null).andExpect(jsonPath("$[*].id", not(hasItem(doA))));
    }

    @Test
    void nomeRepetidoDa409() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();
        criarServico(barbeiro.token(), "Corte", "40.00", "30");

        chamar("POST", "/servicos", barbeiro.token(), "{\"nome\":\"corte\",\"preco\":10,\"duracaoMinutos\":15}")
                .andExpect(status().isConflict());
    }

    @Test
    void rotaPublicaMostraSoOsAtivosESemCusto() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();
        criarServico(barbeiro.token(), "Corte", "40.00", "30");
        String barba = criarServico(barbeiro.token(), "Barba", "30.00", "20");
        chamar("PATCH", "/servicos/" + barba, barbeiro.token(), "{\"ativo\":false}").andExpect(status().isOk());

        chamar("GET", "/barbeiros/publico/" + barbeiro.slug() + "/servicos", null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome").value("Corte"))
                .andExpect(jsonPath("$[0].custoInsumo").doesNotExist());
    }

    @Test
    void rotaPublicaDeSlugInexistenteDa404() throws Exception {
        chamar("GET", "/barbeiros/publico/nao-existe-" + sufixoUnico() + "/servicos", null, null)
                .andExpect(status().isNotFound());
    }
}
