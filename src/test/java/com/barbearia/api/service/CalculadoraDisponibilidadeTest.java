package com.barbearia.api.service;

import com.barbearia.api.service.CalculadoraDisponibilidade.Faixa;
import com.barbearia.api.service.CalculadoraDisponibilidade.Intervalo;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraDisponibilidadeTest {

    private static final LocalDate DIA = LocalDate.of(2026, 10, 21);
    private static final LocalDateTime ONTEM = DIA.minusDays(1).atTime(12, 0);
    private static final Duration PASSO = Duration.ofMinutes(15);
    private static final Duration MEIA_HORA = Duration.ofMinutes(30);

    private static Faixa faixa(String abre, String fecha) {
        return new Faixa(LocalTime.parse(abre), LocalTime.parse(fecha));
    }

    private static Intervalo ocupado(String inicio, String fim) {
        return new Intervalo(DIA.atTime(LocalTime.parse(inicio)), DIA.atTime(LocalTime.parse(fim)));
    }

    private static List<LocalTime> horas(String... valores) {
        return Arrays.stream(valores).map(LocalTime::parse).toList();
    }

    @Test
    void servicoQueEncostaNoFimDoExpedienteCabe() {
        List<LocalTime> livres = CalculadoraDisponibilidade.horariosLivres(DIA, MEIA_HORA,
                List.of(faixa("09:00", "10:00")), List.of(), ONTEM, PASSO);

        assertThat(livres).isEqualTo(horas("09:00", "09:15", "09:30"));
    }

    @Test
    void servicoMaiorQueAFaixaNaoCabe() {
        assertThat(CalculadoraDisponibilidade.horariosLivres(DIA, Duration.ofMinutes(90),
                List.of(faixa("09:00", "10:00")), List.of(), ONTEM, PASSO)).isEmpty();
    }

    @Test
    void horarioOcupadoSomeEOsQueEncostamNeleFicam() {
        List<LocalTime> livres = CalculadoraDisponibilidade.horariosLivres(DIA, MEIA_HORA,
                List.of(faixa("09:00", "11:00")), List.of(ocupado("09:30", "10:00")), ONTEM, PASSO);

        assertThat(livres).isEqualTo(horas("09:00", "10:00", "10:15", "10:30"));
    }

    @Test
    void intervaloDeAlmocoSeparaAsFaixas() {
        List<LocalTime> livres = CalculadoraDisponibilidade.horariosLivres(DIA, Duration.ofMinutes(60),
                List.of(faixa("09:00", "12:00"), faixa("13:00", "14:00")), List.of(), ONTEM, Duration.ofMinutes(60));

        assertThat(livres).isEqualTo(horas("09:00", "10:00", "11:00", "13:00"));
    }

    @Test
    void horarioQueJaPassouNaoAparece() {
        List<LocalTime> livres = CalculadoraDisponibilidade.horariosLivres(DIA, MEIA_HORA,
                List.of(faixa("09:00", "10:00")), List.of(), DIA.atTime(9, 15), PASSO);

        assertThat(livres).isEqualTo(horas("09:30"));
    }

    @Test
    void diaSemFaixaNaoTemHorario() {
        assertThat(CalculadoraDisponibilidade.horariosLivres(DIA, MEIA_HORA, List.of(), List.of(), ONTEM, PASSO))
                .isEmpty();
    }

    @Test
    void dentroDoExpedienteExigeCaberInteiroNumaFaixa() {
        List<Faixa> faixas = List.of(faixa("09:00", "12:00"), faixa("13:00", "18:00"));

        assertThat(CalculadoraDisponibilidade.dentroDoExpediente(
                DIA.atTime(11, 30), DIA.atTime(12, 0), faixas)).isTrue();
        assertThat(CalculadoraDisponibilidade.dentroDoExpediente(
                DIA.atTime(11, 45), DIA.atTime(12, 15), faixas)).isFalse();
        assertThat(CalculadoraDisponibilidade.dentroDoExpediente(
                DIA.atTime(8, 45), DIA.atTime(9, 15), faixas)).isFalse();
    }

    @Test
    void livreDetectaQualquerSobreposicao() {
        List<Intervalo> ocupados = List.of(ocupado("10:00", "10:30"));

        assertThat(CalculadoraDisponibilidade.livre(DIA.atTime(9, 30), DIA.atTime(10, 0), ocupados)).isTrue();
        assertThat(CalculadoraDisponibilidade.livre(DIA.atTime(10, 30), DIA.atTime(11, 0), ocupados)).isTrue();
        assertThat(CalculadoraDisponibilidade.livre(DIA.atTime(9, 45), DIA.atTime(10, 15), ocupados)).isFalse();
        assertThat(CalculadoraDisponibilidade.livre(DIA.atTime(10, 10), DIA.atTime(10, 20), ocupados)).isFalse();
    }
}
