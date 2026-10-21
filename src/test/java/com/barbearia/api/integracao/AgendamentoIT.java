package com.barbearia.api.integracao;

import com.barbearia.api.domain.Agendamento;
import com.barbearia.api.repositories.AgendamentoRepository;
import com.barbearia.api.repositories.BarbeiroRepository;
import com.barbearia.api.repositories.ClienteRepository;
import com.barbearia.api.repositories.ServicoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgendamentoIT extends IntegracaoBase {

    private static final LocalDate AMANHA = LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(1);

    @Autowired
    private AgendamentoRepository agendamentoRepository;
    @Autowired
    private BarbeiroRepository barbeiroRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private ServicoRepository servicoRepository;

    private record Cenario(BarbeiroCriado barbeiro, String servicoId) {
    }

    private Cenario barbeariaAbertaDas6As22() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();
        StringBuilder faixas = new StringBuilder();
        for (String dia : new String[]{"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"}) {
            if (!faixas.isEmpty()) {
                faixas.append(',');
            }
            faixas.append("{\"diaSemana\":\"").append(dia).append("\",\"abre\":\"06:00\",\"fecha\":\"22:00\"}");
        }
        chamar("PUT", "/barbeiros/me/horarios", barbeiro.token(), "{\"faixas\":[" + faixas + "]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)));

        MvcResult servico = chamar("POST", "/servicos", barbeiro.token(),
                "{\"nome\":\"Corte\",\"preco\":40.00,\"duracaoMinutos\":30,\"custoInsumo\":5.00}")
                .andExpect(status().isCreated())
                .andReturn();
        return new Cenario(barbeiro, ler(servico, "$.id"));
    }

    private String clienteDoLink(BarbeiroCriado barbeiro) throws Exception {
        MvcResult cadastro = registrarCliente(barbeiro.barbeiroId(), "Cliente " + sufixoUnico(), telefoneUnico(),
                emailUnico())
                .andExpect(status().isCreated())
                .andReturn();
        return ler(cadastro, "$.token");
    }

    private String pedido(String servicoId, LocalDateTime dataHora) {
        return json("servicoId", servicoId, "dataHora", dataHora.toString());
    }

    private String disponibilidade(Cenario cenario) {
        return "/barbeiros/publico/" + cenario.barbeiro().slug() + "/disponibilidade?servicoId="
                + cenario.servicoId() + "&data=" + AMANHA;
    }

    @Test
    void clienteAgendaOHorarioSomeDaDisponibilidadeEApareceNaAgenda() throws Exception {
        Cenario cenario = barbeariaAbertaDas6As22();
        String cliente = clienteDoLink(cenario.barbeiro());

        chamar("GET", disponibilidade(cenario), null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horarios", hasItem(startsWith("10:00"))));

        chamar("POST", "/agendamentos", cliente, pedido(cenario.servicoId(), AMANHA.atTime(10, 0)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMADO"))
                .andExpect(jsonPath("$.preco").value(40.0));

        chamar("GET", disponibilidade(cenario), null, null)
                .andExpect(jsonPath("$.horarios", not(hasItem(startsWith("10:00")))))
                .andExpect(jsonPath("$.horarios", not(hasItem(startsWith("10:15")))))
                .andExpect(jsonPath("$.horarios", hasItem(startsWith("10:30"))))
                .andExpect(jsonPath("$.horarios", hasItem(startsWith("09:30"))));

        chamar("GET", "/agenda?de=" + AMANHA, cenario.barbeiro().token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].servico.nome").value("Corte"));

        chamar("GET", "/agendamentos", cliente, null)
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void segundoClienteNoMesmoHorarioDa409() throws Exception {
        Cenario cenario = barbeariaAbertaDas6As22();
        chamar("POST", "/agendamentos", clienteDoLink(cenario.barbeiro()),
                pedido(cenario.servicoId(), AMANHA.atTime(10, 0))).andExpect(status().isCreated());

        chamar("POST", "/agendamentos", clienteDoLink(cenario.barbeiro()),
                pedido(cenario.servicoId(), AMANHA.atTime(10, 15)))
                .andExpect(status().isConflict());
    }

    @Test
    void foraDoExpedienteClienteNaoAgendaMasOBarbeiroPode() throws Exception {
        Cenario cenario = barbeariaAbertaDas6As22();
        String cliente = clienteDoLink(cenario.barbeiro());

        chamar("POST", "/agendamentos", cliente, pedido(cenario.servicoId(), AMANHA.atTime(22, 0)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Esse horário está fora do atendimento da barbearia."));

        String clienteId = ler(chamar("GET", "/clientes", cenario.barbeiro().token(), null).andReturn(), "$[0].id");
        chamar("POST", "/agenda", cenario.barbeiro().token(),
                json("clienteId", clienteId, "servicoId", cenario.servicoId(),
                        "dataHora", AMANHA.atTime(22, 0).toString()))
                .andExpect(status().isCreated());
    }

    @Test
    void horarioNoPassadoDa409() throws Exception {
        Cenario cenario = barbeariaAbertaDas6As22();

        chamar("POST", "/agendamentos", clienteDoLink(cenario.barbeiro()),
                pedido(cenario.servicoId(), AMANHA.minusDays(2).atTime(10, 0)))
                .andExpect(status().isConflict());
    }

    @Test
    void remarcarLiberaOHorarioAntigo() throws Exception {
        Cenario cenario = barbeariaAbertaDas6As22();
        String cliente = clienteDoLink(cenario.barbeiro());
        String id = ler(chamar("POST", "/agendamentos", cliente, pedido(cenario.servicoId(), AMANHA.atTime(10, 0)))
                .andExpect(status().isCreated())
                .andReturn(), "$.id");

        chamar("POST", "/agendamentos/" + id + "/remarcar", cliente,
                json("dataHora", AMANHA.atTime(10, 15).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataHora", startsWith(AMANHA + "T10:15")));

        chamar("GET", disponibilidade(cenario), null, null)
                .andExpect(jsonPath("$.horarios", not(hasItem(startsWith("10:15")))))
                .andExpect(jsonPath("$.horarios", hasItem(startsWith("09:45"))));

        chamar("POST", "/agendamentos/" + id + "/cancelar", cliente, null).andExpect(status().isConflict());
    }

    @Test
    void clienteNaoMexeEmAgendamentoDeOutro() throws Exception {
        Cenario cenario = barbeariaAbertaDas6As22();
        String id = ler(chamar("POST", "/agendamentos", clienteDoLink(cenario.barbeiro()),
                pedido(cenario.servicoId(), AMANHA.atTime(10, 0))).andReturn(), "$.id");

        chamar("POST", "/agendamentos/" + id + "/cancelar", clienteDoLink(cenario.barbeiro()), null)
                .andExpect(status().isNotFound());
    }

    @Test
    void clienteComVinculoPendenteNaoAgenda() throws Exception {
        Cenario cenario = barbeariaAbertaDas6As22();
        String telefone = telefoneUnico();
        cadastrarClientePeloBarbeiro(cenario.barbeiro().token(), "Carlos", telefone);
        String pendente = ler(registrarCliente(cenario.barbeiro().barbeiroId(), "Carlos", telefone, emailUnico())
                .andExpect(jsonPath("$.vinculoPendente").value(true))
                .andReturn(), "$.token");

        chamar("POST", "/agendamentos", pendente, pedido(cenario.servicoId(), AMANHA.atTime(10, 0)))
                .andExpect(status().isConflict());
    }

    @Test
    void concluirGeraOMovimentoQueApareceNoResumo() throws Exception {
        Cenario cenario = barbeariaAbertaDas6As22();
        clienteDoLink(cenario.barbeiro());
        String clienteId = ler(chamar("GET", "/clientes", cenario.barbeiro().token(), null).andReturn(), "$[0].id");
        LocalDate ontem = AMANHA.minusDays(2);

        Agendamento passado = agendamentoRepository.save(Agendamento.novo(
                barbeiroRepository.getReferenceById(UUID.fromString(cenario.barbeiro().barbeiroId())),
                clienteRepository.getReferenceById(UUID.fromString(clienteId)),
                servicoRepository.findById(UUID.fromString(cenario.servicoId())).orElseThrow(),
                ontem.atTime(10, 0)));

        chamar("POST", "/agenda/" + passado.getId() + "/concluir", cenario.barbeiro().token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDO"));
        chamar("POST", "/agenda/" + passado.getId() + "/concluir", cenario.barbeiro().token(), null)
                .andExpect(status().isConflict());

        chamar("GET", "/financeiro/resumo?de=" + ontem + "&ate=" + ontem, cenario.barbeiro().token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atendimentos").value(1))
                .andExpect(jsonPath("$.faturamento").value(40.0))
                .andExpect(jsonPath("$.lucro").value(35.0));
    }

    @Test
    void bancoBarraSobreposicaoMesmoSemPassarPeloService() throws Exception {
        Cenario cenario = barbeariaAbertaDas6As22();
        clienteDoLink(cenario.barbeiro());
        String clienteId = ler(chamar("GET", "/clientes", cenario.barbeiro().token(), null).andReturn(), "$[0].id");
        var barbeiro = barbeiroRepository.getReferenceById(UUID.fromString(cenario.barbeiro().barbeiroId()));
        var cliente = clienteRepository.getReferenceById(UUID.fromString(clienteId));
        var servico = servicoRepository.findById(UUID.fromString(cenario.servicoId())).orElseThrow();

        agendamentoRepository.saveAndFlush(Agendamento.novo(barbeiro, cliente, servico, AMANHA.atTime(15, 0)));

        assertThatThrownBy(() -> agendamentoRepository.saveAndFlush(
                Agendamento.novo(barbeiro, cliente, servico, AMANHA.atTime(15, 20))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void horariosComFaixasSobrepostasDa400() throws Exception {
        BarbeiroCriado barbeiro = registrarBarbeiro();

        chamar("PUT", "/barbeiros/me/horarios", barbeiro.token(), "{\"faixas\":["
                + "{\"diaSemana\":\"MONDAY\",\"abre\":\"09:00\",\"fecha\":\"12:00\"},"
                + "{\"diaSemana\":\"MONDAY\",\"abre\":\"11:00\",\"fecha\":\"13:00\"}]}")
                .andExpect(status().isBadRequest());
    }
}
