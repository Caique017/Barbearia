package com.barbearia.api.service;

import com.barbearia.api.domain.Agendamento;
import com.barbearia.api.domain.Barbeiro;
import com.barbearia.api.domain.Cliente;
import com.barbearia.api.domain.Servico;
import com.barbearia.api.domain.Usuario;
import com.barbearia.api.dto.agendamento.AgendamentoBarbeiroRequest;
import com.barbearia.api.dto.agendamento.AgendamentoClienteRequest;
import com.barbearia.api.dto.agendamento.AgendamentoResponse;
import com.barbearia.api.dto.agendamento.MinhaBarbeariaResponse;
import com.barbearia.api.exceptions.AgendamentoNaoEncontradoException;
import com.barbearia.api.exceptions.ClienteNaoEncontradoException;
import com.barbearia.api.exceptions.ClienteSemVinculoException;
import com.barbearia.api.exceptions.HorarioIndisponivelException;
import com.barbearia.api.exceptions.HorarioInvalidoException;
import com.barbearia.api.exceptions.ServicoNaoEncontradoException;
import com.barbearia.api.exceptions.TransicaoAgendamentoInvalidaException;
import com.barbearia.api.repositories.AgendamentoRepository;
import com.barbearia.api.repositories.ClienteRepository;
import com.barbearia.api.repositories.ServicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgendamentoService {

    static final int MAXIMO_DIAS_NA_CONSULTA = 31;

    private final AgendamentoRepository agendamentoRepository;
    private final ClienteRepository clienteRepository;
    private final ServicoRepository servicoRepository;
    private final BarbeiroLogado barbeiroLogado;
    private final DisponibilidadeService disponibilidade;
    private final FinanceiroService financeiro;
    private final RelogioLocal relogio;
    private final AssinaturaService assinaturaService;

    @Transactional
    public AgendamentoResponse agendarPeloBarbeiro(Usuario logado, AgendamentoBarbeiroRequest data) {
        Barbeiro barbeiro = barbeiroLogado.de(logado);
        Cliente cliente = clienteRepository.findByIdAndBarbeiroId(data.clienteId(), barbeiro.getId())
                .orElseThrow(() -> new ClienteNaoEncontradoException("Cliente não encontrado."));

        return resposta(agendar(barbeiro, cliente, data.servicoId(), data.dataHora(), false));
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponse> agenda(Usuario logado, LocalDate de, LocalDate ate) {
        Barbeiro barbeiro = barbeiroLogado.de(logado);
        LocalDate inicio = de != null ? de : relogio.agora().toLocalDate();
        LocalDate fim = ate != null ? ate : inicio;

        if (fim.isBefore(inicio) || ChronoUnit.DAYS.between(inicio, fim) >= MAXIMO_DIAS_NA_CONSULTA) {
            throw new HorarioInvalidoException(
                    "Informe um período de até " + MAXIMO_DIAS_NA_CONSULTA + " dias, com o fim depois do início.");
        }

        return agendamentoRepository.agendaDoBarbeiro(barbeiro.getId(), inicio.atStartOfDay(),
                        fim.plusDays(1).atStartOfDay()).stream()
                .map(this::resposta)
                .toList();
    }

    @Transactional(readOnly = true)
    public AgendamentoResponse detalhe(Usuario logado, UUID id) {
        return resposta(doBarbeiro(logado, id));
    }

    @Transactional
    public AgendamentoResponse concluir(Usuario logado, UUID id) {
        Agendamento agendamento = doBarbeiro(logado, id);
        agendamento.concluir(relogio.agora());
        financeiro.registrarConclusao(agendamento);
        return resposta(agendamento);
    }

    @Transactional
    public AgendamentoResponse marcarFalta(Usuario logado, UUID id) {
        Agendamento agendamento = doBarbeiro(logado, id);
        agendamento.marcarFalta(relogio.agora());
        return resposta(agendamento);
    }

    @Transactional
    public AgendamentoResponse cancelarPeloBarbeiro(Usuario logado, UUID id) {
        Agendamento agendamento = doBarbeiro(logado, id);
        agendamento.cancelar();
        return resposta(agendamento);
    }

    @Transactional
    public AgendamentoResponse remarcarPeloBarbeiro(Usuario logado, UUID id, LocalDateTime novoInicio) {
        return resposta(remarcar(doBarbeiro(logado, id), novoInicio, false));
    }

    @Transactional
    public AgendamentoResponse agendarComoCliente(Usuario logado, AgendamentoClienteRequest data) {
        Cliente cliente = clienteDe(logado);
        assinaturaService.exigirQueAceiteAgendamentos(cliente.getBarbeiro());

        return resposta(agendar(cliente.getBarbeiro(), cliente, data.servicoId(), data.dataHora(), true));
    }

    @Transactional(readOnly = true)
    public MinhaBarbeariaResponse minhaBarbearia(Usuario logado) {
        Barbeiro barbeiro = clienteDe(logado).getBarbeiro();
        return new MinhaBarbeariaResponse(barbeiro.getSlug(), barbeiro.getNomeBarbearia(),
                barbeiro.getHorarioFuncionamento());
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponse> meusAgendamentos(Usuario logado) {
        Cliente cliente = clienteDe(logado);

        return agendamentoRepository.doCliente(cliente.getId()).stream()
                .map(this::resposta)
                .toList();
    }

    @Transactional
    public AgendamentoResponse cancelarComoCliente(Usuario logado, UUID id) {
        Agendamento agendamento = doCliente(logado, id);
        exigirQueNaoComecou(agendamento);
        agendamento.cancelar();
        return resposta(agendamento);
    }

    @Transactional
    public AgendamentoResponse remarcarComoCliente(Usuario logado, UUID id, LocalDateTime novoInicio) {
        Agendamento agendamento = doCliente(logado, id);
        exigirQueNaoComecou(agendamento);
        return resposta(remarcar(agendamento, novoInicio, true));
    }

    private Agendamento agendar(Barbeiro barbeiro, Cliente cliente, UUID servicoId, LocalDateTime inicio,
                                boolean exigirExpediente) {
        Servico servico = servicoRepository.findByIdAndBarbeiroId(servicoId, barbeiro.getId())
                .filter(Servico::isAtivo)
                .orElseThrow(() -> new ServicoNaoEncontradoException("Serviço não encontrado."));

        Agendamento agendamento = Agendamento.novo(barbeiro, cliente, servico, inicio);
        disponibilidade.exigirLivre(barbeiro, agendamento.getDataHora(), agendamento.getTerminaEm(), null,
                exigirExpediente);
        return salvar(agendamento);
    }

    private Agendamento remarcar(Agendamento atual, LocalDateTime novoInicio, boolean exigirExpediente) {
        Agendamento novo = atual.copiaEm(novoInicio);
        disponibilidade.exigirLivre(atual.getBarbeiro(), novo.getDataHora(), novo.getTerminaEm(), atual.getId(),
                exigirExpediente);

        atual.liberarParaRemarcar();
        agendamentoRepository.saveAndFlush(atual);
        salvar(novo);
        atual.setRemarcadoPara(novo);
        return novo;
    }

    private Agendamento salvar(Agendamento agendamento) {
        try {
            return agendamentoRepository.saveAndFlush(agendamento);
        } catch (DataIntegrityViolationException e) {
            throw new HorarioIndisponivelException(DisponibilidadeService.HORARIO_OCUPADO);
        }
    }

    private void exigirQueNaoComecou(Agendamento agendamento) {
        if (agendamento.jaComecou(relogio.agora())) {
            throw new TransicaoAgendamentoInvalidaException(
                    "Esse horário já passou. Fale com a barbearia para mudar.");
        }
    }

    private Agendamento doBarbeiro(Usuario logado, UUID id) {
        Barbeiro barbeiro = barbeiroLogado.de(logado);
        return agendamentoRepository.findByIdAndBarbeiroId(id, barbeiro.getId())
                .orElseThrow(() -> new AgendamentoNaoEncontradoException("Agendamento não encontrado."));
    }

    private Agendamento doCliente(Usuario logado, UUID id) {
        Cliente cliente = clienteDe(logado);
        return agendamentoRepository.findByIdAndClienteId(id, cliente.getId())
                .orElseThrow(() -> new AgendamentoNaoEncontradoException("Agendamento não encontrado."));
    }

    private Cliente clienteDe(Usuario logado) {
        return clienteRepository.findByUsuarioId(logado.getId())
                .orElseThrow(() -> new ClienteSemVinculoException(
                        "Seu cadastro ainda aguarda a aprovação da barbearia."));
    }

    private AgendamentoResponse resposta(Agendamento agendamento) {
        Cliente cliente = agendamento.getCliente();
        return new AgendamentoResponse(
                agendamento.getId(),
                agendamento.getDataHora(),
                agendamento.getTerminaEm(),
                agendamento.getStatus(),
                agendamento.getPreco(),
                new AgendamentoResponse.Servico(agendamento.getServico().getId(), agendamento.getServico().getNome()),
                new AgendamentoResponse.Cliente(cliente.getId(), cliente.getNome(), cliente.getTelefone()));
    }
}
