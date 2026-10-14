package com.barbearia.api.integracao;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClienteIT extends IntegracaoBase {

    @Test
    void barbeiroNaoEnxergaClienteDeOutro() throws Exception {
        BarbeiroCriado a = registrarBarbeiro();
        BarbeiroCriado b = registrarBarbeiro();
        String clienteDoA = cadastrarClientePeloBarbeiro(a.token(), "Carlos", telefoneUnico());

        chamar("GET", "/clientes/" + clienteDoA, b.token(), null).andExpect(status().isNotFound());
        chamar("PATCH", "/clientes/" + clienteDoA, b.token(), json("nome", "Invadido")).andExpect(status().isNotFound());
        chamar("GET", "/clientes", b.token(), null).andExpect(jsonPath("$[*].id", not(hasItem(clienteDoA))));

        chamar("GET", "/clientes/" + clienteDoA, a.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Carlos"));
    }

    @Test
    void telefoneEUnicoPorBarbeiroENaoGlobal() throws Exception {
        BarbeiroCriado a = registrarBarbeiro();
        BarbeiroCriado b = registrarBarbeiro();
        String telefone = telefoneUnico();
        cadastrarClientePeloBarbeiro(a.token(), "Carlos", telefone);

        chamar("POST", "/clientes", a.token(), json("nome", "Outro", "telefone", telefone))
                .andExpect(status().isConflict());
        chamar("POST", "/clientes", b.token(), json("nome", "Carlos", "telefone", telefone))
                .andExpect(status().isCreated());
    }

    @Test
    void barbeiroCorrigeNomeETelefoneDoCliente() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();
        String id = cadastrarClientePeloBarbeiro(barbeiro.token(), "Carls", telefoneUnico());
        String ocupado = telefoneUnico();
        cadastrarClientePeloBarbeiro(barbeiro.token(), "Outro", ocupado);
        String novoTelefone = telefoneUnico();

        chamar("PATCH", "/clientes/" + id, barbeiro.token(), json("nome", "Carlos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Carlos"));
        chamar("PATCH", "/clientes/" + id, barbeiro.token(), json("telefone", ocupado))
                .andExpect(status().isConflict());
        chamar("PATCH", "/clientes/" + id, barbeiro.token(), json("telefone", "123"))
                .andExpect(status().isBadRequest());
        chamar("PATCH", "/clientes/" + id, barbeiro.token(), json("telefone", "(" + novoTelefone.substring(0, 2) + ") "
                + novoTelefone.substring(2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefone").value(novoTelefone))
                .andExpect(jsonPath("$.nome").value("Carlos"));
    }

    @Test
    void autoCadastroComTelefoneNovoJaNasceVinculado() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();
        String telefone = telefoneUnico();

        registrarCliente(barbeiro.barbeiroId(), "Ana", telefone, emailUnico())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vinculoPendente").value(false));

        chamar("GET", "/clientes", barbeiro.token(), null)
                .andExpect(jsonPath("$[?(@.telefone == '" + telefone + "')].temConta").value(true));
    }

    @Test
    void telefoneDeOutraPessoaViraPedidoEOBarbeiroRecusa() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();
        String telefone = telefoneUnico();
        String id = cadastrarClientePeloBarbeiro(barbeiro.token(), "Carlos", telefone);
        String emailInvasor = emailUnico();

        MvcResult pedido = registrarCliente(barbeiro.barbeiroId(), "Invasor", telefone, emailInvasor)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vinculoPendente").value(true))
                .andReturn();
        String tokenInvasor = ler(pedido, "$.token");

        chamar("GET", "/clientes/" + id, barbeiro.token(), null)
                .andExpect(jsonPath("$.nome").value("Carlos"))
                .andExpect(jsonPath("$.temConta").value(false))
                .andExpect(jsonPath("$.pedidoVinculo.nome").value("Invasor"))
                .andExpect(jsonPath("$.pedidoVinculo.email").value(emailInvasor));

        registrarCliente(barbeiro.barbeiroId(), "Outro Invasor", telefone, emailUnico())
                .andExpect(status().isConflict());

        chamar("DELETE", "/clientes/" + id + "/vinculo", barbeiro.token(), null)
                .andExpect(status().isNoContent());

        chamar("GET", "/clientes/" + id, barbeiro.token(), null)
                .andExpect(jsonPath("$.temConta").value(false))
                .andExpect(jsonPath("$.pedidoVinculo").isEmpty());
        chamar("GET", "/auth/me", tokenInvasor, null).andExpect(status().isUnauthorized());
        chamar("POST", "/auth/cliente/login", null, json("email", emailInvasor, "senha", SENHA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void barbeiroAprovaOVinculoDoClienteDeVerdade() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();
        String telefone = telefoneUnico();
        String id = cadastrarClientePeloBarbeiro(barbeiro.token(), "Carlos", telefone);
        registrarCliente(barbeiro.barbeiroId(), "Carlos Silva", telefone, emailUnico())
                .andExpect(status().isCreated());

        chamar("POST", "/clientes/" + id + "/vinculo", barbeiro.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temConta").value(true))
                .andExpect(jsonPath("$.pedidoVinculo").isEmpty())
                .andExpect(jsonPath("$.nome").value("Carlos"));

        chamar("POST", "/clientes/" + id + "/vinculo", barbeiro.token(), null)
                .andExpect(status().isNotFound());
        registrarCliente(barbeiro.barbeiroId(), "Terceiro", telefone, emailUnico())
                .andExpect(status().isConflict());
    }

    @Test
    void barbeiroNaoDecideVinculoDeClienteDeOutro() throws Exception {
        BarbeiroCriado a = registrarBarbeiro();
        BarbeiroCriado b = registrarBarbeiro();
        String telefone = telefoneUnico();
        String id = cadastrarClientePeloBarbeiro(a.token(), "Carlos", telefone);
        registrarCliente(a.barbeiroId(), "Carlos", telefone, emailUnico()).andExpect(status().isCreated());

        chamar("POST", "/clientes/" + id + "/vinculo", b.token(), null).andExpect(status().isNotFound());
        chamar("DELETE", "/clientes/" + id + "/vinculo", b.token(), null).andExpect(status().isNotFound());
    }
}
