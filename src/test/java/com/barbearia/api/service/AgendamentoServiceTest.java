package com.barbearia.api.service;

import com.barbearia.api.domain.Agendamento;
import com.barbearia.api.domain.Barbeiro;
import com.barbearia.api.domain.Cliente;
import com.barbearia.api.domain.Servico;
import com.barbearia.api.domain.Usuario;
import com.barbearia.api.dto.agendamento.AgendamentoClienteRequest;
import com.barbearia.api.enums.StatusAgendamento;
import com.barbearia.api.exceptions.ClienteSemVinculoException;
import com.barbearia.api.exceptions.HorarioIndisponivelException;
import com.barbearia.api.exceptions.HorarioInvalidoException;
import com.barbearia.api.exceptions.ServicoNaoEncontradoException;
import com.barbearia.api.exceptions.TransicaoAgendamentoInvalidaException;
import com.barbearia.api.repositories.AgendamentoRepository;
import com.barbearia.api.repositories.ClienteRepository;
import com.barbearia.api.repositories.ServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 20, 12, 0);
    private static final LocalDateTime AMANHA_10H = LocalDateTime.of(2026, 10, 21, 10, 0);

    @Mock
    private AgendamentoRepository agendamentoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ServicoRepository servicoRepository;
    @Mock
    private BarbeiroLogado barbeiroLogado;
    @Mock
    private DisponibilidadeService disponibilidade;
    @Mock
    private FinanceiroService financeiro;
    @Mock
    private RelogioLocal relogio;
    @Mock
    private AssinaturaService assinaturaService;

    @InjectMocks
    private AgendamentoService service;

    private final Usuario usuario = Usuario.builder().id(UUID.randomUUID()).build();
    private Barbeiro barbeiro;
    private Cliente cliente;
    private Servico corte;

    @BeforeEach
    void cenario() {
        barbeiro = Barbeiro.builder().id(UUID.randomUUID()).build();
        cliente = Cliente.builder().id(UUID.randomUUID()).barbeiro(barbeiro).nome("Carlos").build();
        corte = Servico.builder().id(UUID.randomUUID()).barbeiro(barbeiro).nome("Corte")
                .preco(new BigDecimal("40.00")).custoInsumo(BigDecimal.ZERO).duracaoMinutos(30).build();
    }

    private void clienteComVinculo() {
        when(clienteRepository.findByUsuarioId(usuario.getId())).thenReturn(Optional.of(cliente));
    }

    private Agendamento agendamentoDoCliente(LocalDateTime inicio) {
        Agendamento agendamento = Agendamento.novo(barbeiro, cliente, corte, inicio);
        agendamento.setId(UUID.randomUUID());
        when(agendamentoRepository.findByIdAndClienteId(agendamento.getId(), cliente.getId()))
                .thenReturn(Optional.of(agendamento));
        return agendamento;
    }

    @Test
    void clienteSemVinculoNaoAgenda() {
        when(clienteRepository.findByUsuarioId(usuario.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.agendarComoCliente(usuario,
                new AgendamentoClienteRequest(corte.getId(), AMANHA_10H)))
                .isInstanceOf(ClienteSemVinculoException.class);
    }

    @Test
    void clienteAgendaNoBarbeiroDeleExigindoExpediente() {
        clienteComVinculo();
        when(servicoRepository.findByIdAndBarbeiroId(corte.getId(), barbeiro.getId())).thenReturn(Optional.of(corte));
        when(agendamentoRepository.saveAndFlush(any())).thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = service.agendarComoCliente(usuario, new AgendamentoClienteRequest(corte.getId(), AMANHA_10H));

        verify(disponibilidade).exigirLivre(barbeiro, AMANHA_10H, AMANHA_10H.plusMinutes(30), null, true);
        assertThat(resposta.status()).isEqualTo(StatusAgendamento.CONFIRMADO);
        assertThat(resposta.terminaEm()).isEqualTo(AMANHA_10H.plusMinutes(30));
    }

    @Test
    void servicoDesativadoNaoPodeSerAgendado() {
        clienteComVinculo();
        corte.setAtivo(false);
        when(servicoRepository.findByIdAndBarbeiroId(corte.getId(), barbeiro.getId())).thenReturn(Optional.of(corte));

        assertThatThrownBy(() -> service.agendarComoCliente(usuario,
                new AgendamentoClienteRequest(corte.getId(), AMANHA_10H)))
                .isInstanceOf(ServicoNaoEncontradoException.class);
    }

    @Test
    void corridaPerdidaNoBancoViraHorarioIndisponivel() {
        clienteComVinculo();
        when(servicoRepository.findByIdAndBarbeiroId(corte.getId(), barbeiro.getId())).thenReturn(Optional.of(corte));
        when(agendamentoRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("23P01"));

        assertThatThrownBy(() -> service.agendarComoCliente(usuario,
                new AgendamentoClienteRequest(corte.getId(), AMANHA_10H)))
                .isInstanceOf(HorarioIndisponivelException.class);
    }

    @Test
    void clienteNaoCancelaHorarioQueJaPassou() {
        clienteComVinculo();
        Agendamento agendamento = agendamentoDoCliente(AGORA.minusHours(1));
        when(relogio.agora()).thenReturn(AGORA);

        assertThatThrownBy(() -> service.cancelarComoCliente(usuario, agendamento.getId()))
                .isInstanceOf(TransicaoAgendamentoInvalidaException.class);
        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
    }

    @Test
    void remarcarLiberaOAntigoEApontaParaONovo() {
        clienteComVinculo();
        Agendamento antigo = agendamentoDoCliente(AMANHA_10H);
        when(relogio.agora()).thenReturn(AGORA);
        when(agendamentoRepository.saveAndFlush(any())).thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = service.remarcarComoCliente(usuario, antigo.getId(), AMANHA_10H.plusHours(2));

        verify(disponibilidade).exigirLivre(eq(barbeiro), eq(AMANHA_10H.plusHours(2)),
                eq(AMANHA_10H.plusHours(2).plusMinutes(30)), eq(antigo.getId()), eq(true));
        assertThat(antigo.getStatus()).isEqualTo(StatusAgendamento.REMARCADO);
        assertThat(antigo.getRemarcadoPara()).isNotNull();
        assertThat(resposta.dataHora()).isEqualTo(AMANHA_10H.plusHours(2));
    }

    @Test
    void remarcarParaHorarioOcupadoNaoMexeNoAntigo() {
        clienteComVinculo();
        Agendamento antigo = agendamentoDoCliente(AMANHA_10H);
        when(relogio.agora()).thenReturn(AGORA);
        doThrow(new HorarioIndisponivelException("ocupado")).when(disponibilidade)
                .exigirLivre(any(), any(), any(), any(), eq(true));

        assertThatThrownBy(() -> service.remarcarComoCliente(usuario, antigo.getId(), AMANHA_10H.plusHours(2)))
                .isInstanceOf(HorarioIndisponivelException.class);
        assertThat(antigo.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
        verify(agendamentoRepository, never()).saveAndFlush(any());
    }

    @Test
    void concluirRegistraOMovimentoFinanceiro() {
        when(barbeiroLogado.de(usuario)).thenReturn(barbeiro);
        Agendamento agendamento = Agendamento.novo(barbeiro, cliente, corte, AGORA.minusHours(1));
        agendamento.setId(UUID.randomUUID());
        when(agendamentoRepository.findByIdAndBarbeiroId(agendamento.getId(), barbeiro.getId()))
                .thenReturn(Optional.of(agendamento));
        when(relogio.agora()).thenReturn(AGORA);

        service.concluir(usuario, agendamento.getId());

        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CONCLUIDO);
        verify(financeiro).registrarConclusao(agendamento);
    }

    @Test
    void agendaComPeriodoInvertidoOuLongoDemaisDa400() {
        when(barbeiroLogado.de(usuario)).thenReturn(barbeiro);
        LocalDate hoje = AGORA.toLocalDate();

        assertThatThrownBy(() -> service.agenda(usuario, hoje, hoje.minusDays(1)))
                .isInstanceOf(HorarioInvalidoException.class);
        assertThatThrownBy(() -> service.agenda(usuario, hoje, hoje.plusDays(31)))
                .isInstanceOf(HorarioInvalidoException.class);
    }
}
