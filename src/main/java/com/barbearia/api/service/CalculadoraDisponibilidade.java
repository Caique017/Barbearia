package com.barbearia.api.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.TreeSet;

public final class CalculadoraDisponibilidade {

    private CalculadoraDisponibilidade() {
    }

    public record Faixa(LocalTime abre, LocalTime fecha) {
    }

    public record Intervalo(LocalDateTime inicio, LocalDateTime fim) {

        boolean sobrepoe(LocalDateTime outroInicio, LocalDateTime outroFim) {
            return inicio.isBefore(outroFim) && fim.isAfter(outroInicio);
        }
    }

    public static List<LocalTime> horariosLivres(LocalDate data, Duration duracao, List<Faixa> faixas,
                                                 List<Intervalo> ocupados, LocalDateTime agora, Duration passo) {
        TreeSet<LocalTime> livres = new TreeSet<>();
        for (Faixa faixa : faixas) {
            LocalDateTime candidato = data.atTime(faixa.abre());
            LocalDateTime fechamento = data.atTime(faixa.fecha());
            while (!candidato.plus(duracao).isAfter(fechamento)) {
                if (candidato.isAfter(agora) && livre(candidato, candidato.plus(duracao), ocupados)) {
                    livres.add(candidato.toLocalTime());
                }
                candidato = candidato.plus(passo);
            }
        }
        return List.copyOf(livres);
    }

    public static boolean dentroDoExpediente(LocalDateTime inicio, LocalDateTime fim, List<Faixa> faixas) {
        if (!inicio.toLocalDate().equals(fim.toLocalDate()) && !fim.toLocalTime().equals(LocalTime.MIDNIGHT)) {
            return false;
        }
        return faixas.stream().anyMatch(faixa ->
                !inicio.toLocalTime().isBefore(faixa.abre())
                        && !fim.isAfter(inicio.toLocalDate().atTime(faixa.fecha())));
    }

    public static boolean livre(LocalDateTime inicio, LocalDateTime fim, List<Intervalo> ocupados) {
        return ocupados.stream().noneMatch(ocupado -> ocupado.sobrepoe(inicio, fim));
    }
}
