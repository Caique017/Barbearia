package com.barbearia.api.integracao;

import com.barbearia.api.domain.Assinatura;
import com.barbearia.api.domain.Usuario;
import com.barbearia.api.enums.StatusAgendamento;
import com.barbearia.api.repositories.AgendamentoRepository;
import com.barbearia.api.repositories.AssinaturaRepository;
import com.barbearia.api.repositories.UsuarioRepository;
import com.barbearia.api.security.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContaIT extends IntegracaoBase {

    private static final LocalDateTime AMANHA_AS_10 = LocalDate.now(ZoneId.of("America/Sao_Paulo"))
            .plusDays(1).atTime(10, 0);

    @Autowired
    private AgendamentoRepository agendamentoRepository;
    @Autowired
    private AssinaturaRepository assinaturaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private TokenService tokenService;

    private record ClienteComConta(String token, String email, String clienteId) {
    }

    private ClienteComConta clienteDoLink(BarbeiroCriado barbeiro) throws Exception {
        String email = emailUnico();
        String telefone = telefoneUnico();
        MvcResult cadastro = registrarCliente(barbeiro.barbeiroId(), "Carlos", telefone, email)
                .andExpect(status().isCreated())
                .andReturn();
        MvcResult lista = chamar("GET", "/clientes", barbeiro.token(), null).andReturn();
        String clienteId = ler(lista, "$[?(@.telefone == '" + telefone + "')].id").replaceAll("[\\[\\]\"]", "");
        return new ClienteComConta(ler(cadastro, "$.token"), email, clienteId);
    }

    private String servicoComHorarioAberto(BarbeiroCriado barbeiro) throws Exception {
        String dia = AMANHA_AS_10.getDayOfWeek().name();
        chamar("PUT", "/barbeiros/me/horarios", barbeiro.token(),
                "{\"faixas\":[{\"diaSemana\":\"" + dia + "\",\"abre\":\"08:00\",\"fecha\":\"20:00\"}]}")
                .andExpect(status().isOk());
        MvcResult servico = chamar("POST", "/servicos", barbeiro.token(),
                "{\"nome\":\"Corte\",\"preco\":40.00,\"duracaoMinutos\":30}")
                .andExpect(status().isCreated())
                .andReturn();
        return ler(servico, "$.id");
    }

    private String agendarComoCliente(String tokenCliente, String servicoId) throws Exception {
        MvcResult agendamento = chamar("POST", "/agendamentos", tokenCliente,
                json("servicoId", servicoId, "dataHora", AMANHA_AS_10.toString()))
                .andExpect(status().isCreated())
                .andReturn();
        return ler(agendamento, "$.id");
    }

    private String senha(String valor) {
        return json("senha", valor);
    }

    @Test
    void cadastroSemAceiteDosTermosDa400() throws Exception {
        chamar("POST", "/auth/barbeiro/registro", null, json("nome", "Pessoa", "nomeBarbearia", "Barbearia",
                "email", emailUnico(), "senha", SENHA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("aceiteTermos"));
    }

    @Test
    void cadastroGuardaAVersaoDosTermosEMarketingSoComOptIn() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();

        chamar("GET", "/conta/dados", barbeiro.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conta.termosVersao").isNotEmpty())
                .andExpect(jsonPath("$.conta.termosAceitosEm").isNotEmpty())
                .andExpect(jsonPath("$.conta.marketingAceitoEm").doesNotExist());
    }

    @Test
    void barbeiroExportaOsProprioDadosComOsClientes() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();
        cadastrarClientePeloBarbeiro(barbeiro.token(), "Carlos", telefoneUnico());

        chamar("GET", "/conta/dados", barbeiro.token(), null)
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andExpect(jsonPath("$.conta.email").value(barbeiro.email()))
                .andExpect(jsonPath("$.conta.tipo").value("BARBEIRO"))
                .andExpect(jsonPath("$.barbearia.slug").value(barbeiro.slug()))
                .andExpect(jsonPath("$.barbearia.assinatura.status").value("TESTE"))
                .andExpect(jsonPath("$.barbearia.clientes", hasSize(1)))
                .andExpect(jsonPath("$.barbearia.clientes[0].nome").value("Carlos"))
                .andExpect(jsonPath("$.cadastroCliente").doesNotExist());
    }

    @Test
    void clienteExportaOCadastroEOsAgendamentos() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();
        String servicoId = servicoComHorarioAberto(barbeiro);
        ClienteComConta cliente = clienteDoLink(barbeiro);
        agendarComoCliente(cliente.token(), servicoId);

        chamar("GET", "/conta/dados", cliente.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conta.tipo").value("CLIENTE"))
                .andExpect(jsonPath("$.cadastroCliente.nome").value("Carlos"))
                .andExpect(jsonPath("$.cadastroCliente.agendamentos", hasSize(1)))
                .andExpect(jsonPath("$.cadastroCliente.agendamentos[0].servico").value("Corte"))
                .andExpect(jsonPath("$.barbearia").doesNotExist());
    }

    @Test
    void senhaErradaDa400ENaoDesloga() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();

        chamar("POST", "/conta/excluir", barbeiro.token(), senha("errada123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Senha incorreta."));

        chamar("GET", "/auth/me", barbeiro.token(), null).andExpect(status().isOk());
    }

    @Test
    void barbeiroExcluiAContaEApagaABarbeariaEAsContasDosClientes() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();
        String servicoId = servicoComHorarioAberto(barbeiro);
        ClienteComConta cliente = clienteDoLink(barbeiro);
        agendarComoCliente(cliente.token(), servicoId);

        chamar("POST", "/conta/excluir", barbeiro.token(), senha(SENHA)).andExpect(status().isNoContent());

        chamar("GET", "/auth/me", barbeiro.token(), null).andExpect(status().isUnauthorized());
        chamar("GET", "/auth/me", cliente.token(), null).andExpect(status().isUnauthorized());
        chamar("GET", "/barbeiros/publico/" + barbeiro.slug(), null, null).andExpect(status().isNotFound());
        assertThat(usuarioRepository.findByEmail(barbeiro.email())).isEmpty();
        assertThat(usuarioRepository.findByEmail(cliente.email())).isEmpty();
        assertThat(agendamentoRepository.findAll())
                .noneMatch(a -> a.getServico().getId().toString().equals(servicoId));
    }

    @Test
    void clienteExcluiAContaCancelaOsFuturosEOCadastroFicaComABarbearia() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();
        String servicoId = servicoComHorarioAberto(barbeiro);
        ClienteComConta cliente = clienteDoLink(barbeiro);
        String agendamentoId = agendarComoCliente(cliente.token(), servicoId);

        chamar("POST", "/conta/excluir", cliente.token(), senha(SENHA)).andExpect(status().isNoContent());

        chamar("GET", "/auth/me", cliente.token(), null).andExpect(status().isUnauthorized());
        chamar("GET", "/clientes/" + cliente.clienteId(), barbeiro.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Carlos"))
                .andExpect(jsonPath("$.temConta").value(false));
        assertThat(agendamentoRepository.findById(UUID.fromString(agendamentoId)).orElseThrow().getStatus())
                .isEqualTo(StatusAgendamento.CANCELADO);
    }

    @Test
    void anonimizarApagaOsDadosAContaECancelaOsFuturosMesmoComAssinaturaVencida() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();
        String servicoId = servicoComHorarioAberto(barbeiro);
        ClienteComConta cliente = clienteDoLink(barbeiro);
        String agendamentoId = agendarComoCliente(cliente.token(), servicoId);
        Assinatura assinatura = assinaturaRepository.findByBarbeiroId(UUID.fromString(barbeiro.barbeiroId()))
                .orElseThrow();
        assinatura.setTesteAte(Instant.now().minus(Duration.ofDays(1)));
        assinaturaRepository.save(assinatura);

        chamar("POST", "/clientes/" + cliente.clienteId() + "/anonimizar", barbeiro.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Cliente removido"))
                .andExpect(jsonPath("$.telefone").value("removido-" + cliente.clienteId()))
                .andExpect(jsonPath("$.temConta").value(false));

        chamar("GET", "/auth/me", cliente.token(), null).andExpect(status().isUnauthorized());
        assertThat(usuarioRepository.findByEmail(cliente.email())).isEmpty();
        assertThat(agendamentoRepository.findById(UUID.fromString(agendamentoId)).orElseThrow().getStatus())
                .isEqualTo(StatusAgendamento.CANCELADO);
    }

    @Test
    void anonimizarClienteDeOutroBarbeiroDa404() throws Exception {
        BarbeiroCriado dono = registrarBarbeiro();
        BarbeiroCriado outro = registrarBarbeiro();
        String clienteId = cadastrarClientePeloBarbeiro(dono.token(), "Carlos", telefoneUnico());

        chamar("POST", "/clientes/" + clienteId + "/anonimizar", outro.token(), null)
                .andExpect(status().isNotFound());
    }

    @Test
    void preferenciasLigamEDesligamMarketingELembretes() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();

        chamar("GET", "/conta/preferencias", barbeiro.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marketing").value(false))
                .andExpect(jsonPath("$.lembretes").value(true));

        chamar("PATCH", "/conta/preferencias", barbeiro.token(), "{\"marketing\":true,\"lembretes\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marketing").value(true))
                .andExpect(jsonPath("$.lembretes").value(false));
    }

    @Test
    void descadastroPeloLinkDoEmailFuncionaSemLogin() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();
        Usuario usuario = usuarioRepository.findByEmail(barbeiro.email()).orElseThrow();

        chamar("POST", "/conta/descadastro", null, json("token", tokenService.gerarTokenDescadastro(usuario)))
                .andExpect(status().isNoContent());

        chamar("GET", "/conta/preferencias", barbeiro.token(), null)
                .andExpect(jsonPath("$.lembretes").value(false));
        chamar("POST", "/conta/descadastro", null, json("token", "invalido"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void contaExigeLogin() throws Exception {
        chamar("GET", "/conta/dados", null, null).andExpect(status().isUnauthorized());
        chamar("POST", "/conta/excluir", null, senha(SENHA)).andExpect(status().isUnauthorized());
    }
}
