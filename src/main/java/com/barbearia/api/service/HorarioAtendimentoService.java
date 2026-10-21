package com.barbearia.api.service;

import com.barbearia.api.domain.Barbeiro;
import com.barbearia.api.domain.BarbeiroHorario;
import com.barbearia.api.domain.Usuario;
import com.barbearia.api.dto.agendamento.FaixaHorario;
import com.barbearia.api.dto.agendamento.HorariosRequest;
import com.barbearia.api.exceptions.HorarioInvalidoException;
import com.barbearia.api.repositories.BarbeiroHorarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HorarioAtendimentoService {

    private final BarbeiroHorarioRepository horarioRepository;
    private final BarbeiroLogado barbeiroLogado;

    @Transactional(readOnly = true)
    public List<FaixaHorario> listar(Usuario logado) {
        Barbeiro barbeiro = barbeiroLogado.de(logado);

        return horarioRepository.doBarbeiro(barbeiro.getId()).stream()
                .map(horario -> new FaixaHorario(horario.getDiaSemana(), horario.getAbre(), horario.getFecha()))
                .toList();
    }

    @Transactional
    public List<FaixaHorario> substituir(Usuario logado, HorariosRequest data) {
        Barbeiro barbeiro = barbeiroLogado.de(logado);
        List<FaixaHorario> faixas = data.faixas().stream()
                .sorted(Comparator.comparing(FaixaHorario::diaSemana).thenComparing(FaixaHorario::abre))
                .toList();

        validar(faixas);

        horarioRepository.apagarDoBarbeiro(barbeiro.getId());
        horarioRepository.saveAll(faixas.stream()
                .map(faixa -> BarbeiroHorario.builder()
                        .barbeiro(barbeiro)
                        .diaSemana(faixa.diaSemana())
                        .abre(faixa.abre())
                        .fecha(faixa.fecha())
                        .build())
                .toList());

        return faixas;
    }

    private void validar(List<FaixaHorario> faixasOrdenadas) {
        FaixaHorario anterior = null;
        for (FaixaHorario faixa : faixasOrdenadas) {
            if (!faixa.abre().isBefore(faixa.fecha())) {
                throw new HorarioInvalidoException("Em cada faixa, o horário de abrir precisa ser antes do de fechar.");
            }
            if (anterior != null && anterior.diaSemana() == faixa.diaSemana()
                    && faixa.abre().isBefore(anterior.fecha())) {
                throw new HorarioInvalidoException("Há faixas sobrepostas no mesmo dia.");
            }
            anterior = faixa;
        }
    }
}
