package com.barbearia.api.service;

import com.barbearia.api.domain.Agendamento;
import com.barbearia.api.domain.Barbeiro;
import com.barbearia.api.domain.BloqueioHorario;
import com.barbearia.api.domain.Usuario;
import com.barbearia.api.dto.agendamento.BloqueioRequest;
import com.barbearia.api.dto.agendamento.BloqueioResponse;
import com.barbearia.api.exceptions.BloqueioNaoEncontradoException;
import com.barbearia.api.exceptions.HorarioInvalidoException;
import com.barbearia.api.repositories.AgendamentoRepository;
import com.barbearia.api.repositories.BloqueioHorarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BloqueioServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 20, 12, 0);

    @Mock
    private BloqueioHorarioRepository bloqueioRepository;
    @Mock
    private AgendamentoRepository agendamentoRepository;
    @Mock
    private BarbeiroLogado barbeiroLogado;
    @Mock
    private RelogioLocal relogio;

    @InjectMocks
    private BloqueioService service;

    private final Usuario logado = new Usuario();
    private final Barbeiro barbeiro = Barbeiro.builder().id(UUID.randomUUID()).build();

    @BeforeEach
    void barbeiroLogado() {
        when(barbeiroLogado.de(logado)).thenReturn(barbeiro);
    }

    @Test
    void fimAntesDoInicioDa400() {
        assertThatThrownBy(() -> service.criar(logado,
                new BloqueioRequest(AGORA.plusDays(1), AGORA.plusDays(1).minusHours(1), null)))
                .isInstanceOf(HorarioInvalidoException.class);
        verify(bloqueioRepository, never()).save(any());
    }

    @Test
    void bloqueioQueJaTerminouDa400() {
        when(relogio.agora()).thenReturn(AGORA);

        assertThatThrownBy(() -> service.criar(logado,
                new BloqueioRequest(AGORA.minusDays(2), AGORA.minusDays(1), null)))
                .isInstanceOf(HorarioInvalidoException.class)
                .hasMessageContaining("futuro");
    }

    @Test
    void bloqueioDeMaisDeUmAnoDa400() {
        when(relogio.agora()).thenReturn(AGORA);

        assertThatThrownBy(() -> service.criar(logado,
                new BloqueioRequest(AGORA.plusDays(1), AGORA.plusDays(400), null)))
                .isInstanceOf(HorarioInvalidoException.class);
    }

    @Test
    void criarAvisaQuantosAgendamentosCaemNoPeriodo() {
        when(relogio.agora()).thenReturn(AGORA);
        when(bloqueioRepository.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
        LocalDateTime inicio = AGORA.plusDays(1).withHour(14);
        when(agendamentoRepository.sobrepostos(barbeiro.getId(), inicio, inicio.plusHours(1),
                AgendamentoRepository.OCUPAM_HORARIO))
                .thenReturn(List.of(mock(Agendamento.class)));

        BloqueioResponse resposta = service.criar(logado, new BloqueioRequest(inicio, inicio.plusHours(1), " Médico "));

        assertThat(resposta.agendamentosNoPeriodo()).isEqualTo(1);
        assertThat(resposta.motivo()).isEqualTo("Médico");
    }

    @Test
    void naoRemoveBloqueioDeOutroBarbeiro() {
        UUID deOutro = UUID.randomUUID();

        assertThatThrownBy(() -> service.remover(logado, deOutro))
                .isInstanceOf(BloqueioNaoEncontradoException.class);
        verify(bloqueioRepository, never()).delete(any(BloqueioHorario.class));
    }
}
