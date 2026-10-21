package com.barbearia.api.service;

import com.barbearia.api.domain.Barbeiro;
import com.barbearia.api.domain.Usuario;
import com.barbearia.api.dto.agendamento.FaixaHorario;
import com.barbearia.api.dto.agendamento.HorariosRequest;
import com.barbearia.api.exceptions.HorarioInvalidoException;
import com.barbearia.api.repositories.BarbeiroHorarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HorarioAtendimentoServiceTest {

    @Mock
    private BarbeiroHorarioRepository horarioRepository;
    @Mock
    private BarbeiroLogado barbeiroLogado;

    @InjectMocks
    private HorarioAtendimentoService service;

    private final Usuario logado = new Usuario();
    private final Barbeiro barbeiro = Barbeiro.builder().id(UUID.randomUUID()).build();

    @BeforeEach
    void barbeiroLogado() {
        when(barbeiroLogado.de(logado)).thenReturn(barbeiro);
    }

    private static FaixaHorario faixa(DayOfWeek dia, String abre, String fecha) {
        return new FaixaHorario(dia, LocalTime.parse(abre), LocalTime.parse(fecha));
    }

    @Test
    void substituiTudoEDevolveOrdenado() {
        List<FaixaHorario> salvas = service.substituir(logado, new HorariosRequest(List.of(
                faixa(DayOfWeek.TUESDAY, "09:00", "18:00"),
                faixa(DayOfWeek.MONDAY, "13:00", "18:00"),
                faixa(DayOfWeek.MONDAY, "09:00", "12:00"))));

        verify(horarioRepository).apagarDoBarbeiro(barbeiro.getId());
        assertThat(salvas).extracting(FaixaHorario::diaSemana, FaixaHorario::abre).containsExactly(
                tuple(DayOfWeek.MONDAY, LocalTime.of(9, 0)),
                tuple(DayOfWeek.MONDAY, LocalTime.of(13, 0)),
                tuple(DayOfWeek.TUESDAY, LocalTime.of(9, 0)));
    }

    @Test
    void faixaQueFechaAntesDeAbrirDa400SemApagarNada() {
        assertThatThrownBy(() -> service.substituir(logado, new HorariosRequest(List.of(
                faixa(DayOfWeek.MONDAY, "18:00", "09:00")))))
                .isInstanceOf(HorarioInvalidoException.class);
        verify(horarioRepository, never()).apagarDoBarbeiro(any());
    }

    @Test
    void faixasSobrepostasNoMesmoDiaDa400() {
        assertThatThrownBy(() -> service.substituir(logado, new HorariosRequest(List.of(
                faixa(DayOfWeek.MONDAY, "09:00", "12:00"),
                faixa(DayOfWeek.MONDAY, "11:00", "14:00")))))
                .isInstanceOf(HorarioInvalidoException.class)
                .hasMessageContaining("sobrepostas");
    }

    @Test
    void mesmaHoraEmDiasDiferentesPode() {
        service.substituir(logado, new HorariosRequest(List.of(
                faixa(DayOfWeek.MONDAY, "09:00", "12:00"),
                faixa(DayOfWeek.TUESDAY, "09:00", "12:00"))));

        verify(horarioRepository).saveAll(any());
    }
}
