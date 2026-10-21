package com.barbearia.api.service;

import com.barbearia.api.domain.Barbeiro;
import com.barbearia.api.domain.Servico;
import com.barbearia.api.dto.agendamento.DisponibilidadeResponse;
import com.barbearia.api.exceptions.BarbeiroNaoEncontradoException;
import com.barbearia.api.exceptions.HorarioIndisponivelException;
import com.barbearia.api.exceptions.ServicoNaoEncontradoException;
import com.barbearia.api.repositories.AgendamentoRepository;
import com.barbearia.api.repositories.BarbeiroHorarioRepository;
import com.barbearia.api.repositories.BarbeiroRepository;
import com.barbearia.api.repositories.BloqueioHorarioRepository;
import com.barbearia.api.repositories.ServicoRepository;
import com.barbearia.api.service.CalculadoraDisponibilidade.Faixa;
import com.barbearia.api.service.CalculadoraDisponibilidade.Intervalo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class DisponibilidadeService {

    static final String HORARIO_OCUPADO = "Esse horário não está mais livre. Escolha outro.";

    private final BarbeiroRepository barbeiroRepository;
    private final ServicoRepository servicoRepository;
    private final BarbeiroHorarioRepository horarioRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final BloqueioHorarioRepository bloqueioRepository;
    private final RelogioLocal relogio;
    private final Duration passo;
    private final int diasDeAntecedencia;

    public DisponibilidadeService(BarbeiroRepository barbeiroRepository,
                                  ServicoRepository servicoRepository,
                                  BarbeiroHorarioRepository horarioRepository,
                                  AgendamentoRepository agendamentoRepository,
                                  BloqueioHorarioRepository bloqueioRepository,
                                  RelogioLocal relogio,
                                  @Value("${api.agenda.passo-minutos:15}") int passoMinutos,
                                  @Value("${api.agenda.antecedencia-maxima-dias:60}") int diasDeAntecedencia) {
        this.barbeiroRepository = barbeiroRepository;
        this.servicoRepository = servicoRepository;
        this.horarioRepository = horarioRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.bloqueioRepository = bloqueioRepository;
        this.relogio = relogio;
        this.passo = Duration.ofMinutes(passoMinutos);
        this.diasDeAntecedencia = diasDeAntecedencia;
    }

    @Transactional(readOnly = true)
    public DisponibilidadeResponse doDia(String slug, UUID servicoId, LocalDate data) {
        Barbeiro barbeiro = barbeiroRepository.findBySlug(slug)
                .orElseThrow(() -> new BarbeiroNaoEncontradoException("Barbearia não encontrada."));
        Servico servico = servicoRepository.findByIdAndBarbeiroId(servicoId, barbeiro.getId())
                .filter(Servico::isAtivo)
                .orElseThrow(() -> new ServicoNaoEncontradoException("Serviço não encontrado."));

        LocalDateTime agora = relogio.agora();
        if (data.isBefore(agora.toLocalDate()) || data.isAfter(limite(agora).toLocalDate())) {
            return new DisponibilidadeResponse(data, servicoId, List.of());
        }

        List<LocalTime> horarios = CalculadoraDisponibilidade.horariosLivres(
                data,
                Duration.ofMinutes(servico.getDuracaoMinutos()),
                faixas(barbeiro, data),
                ocupados(barbeiro, data.atStartOfDay(), data.plusDays(1).atStartOfDay(), null),
                agora,
                passo);

        return new DisponibilidadeResponse(data, servicoId, horarios);
    }

    public void exigirLivre(Barbeiro barbeiro, LocalDateTime inicio, LocalDateTime fim, UUID ignorar,
                            boolean exigirExpediente) {
        LocalDateTime agora = relogio.agora();
        if (!inicio.isAfter(agora)) {
            throw new HorarioIndisponivelException("Escolha um horário no futuro.");
        }
        if (inicio.isAfter(limite(agora))) {
            throw new HorarioIndisponivelException(
                    "Só dá para agendar com até " + diasDeAntecedencia + " dias de antecedência.");
        }
        if (exigirExpediente
                && !CalculadoraDisponibilidade.dentroDoExpediente(inicio, fim, faixas(barbeiro, inicio.toLocalDate()))) {
            throw new HorarioIndisponivelException("Esse horário está fora do atendimento da barbearia.");
        }
        if (!CalculadoraDisponibilidade.livre(inicio, fim, ocupados(barbeiro, inicio, fim, ignorar))) {
            throw new HorarioIndisponivelException(HORARIO_OCUPADO);
        }
    }

    private LocalDateTime limite(LocalDateTime agora) {
        return agora.plusDays(diasDeAntecedencia);
    }

    private List<Faixa> faixas(Barbeiro barbeiro, LocalDate data) {
        return horarioRepository.findByBarbeiroIdAndDiaSemanaOrderByAbre(barbeiro.getId(), data.getDayOfWeek())
                .stream()
                .map(horario -> new Faixa(horario.getAbre(), horario.getFecha()))
                .toList();
    }

    private List<Intervalo> ocupados(Barbeiro barbeiro, LocalDateTime de, LocalDateTime ate, UUID ignorar) {
        Stream<Intervalo> agendamentos = agendamentoRepository
                .sobrepostos(barbeiro.getId(), de, ate, AgendamentoRepository.OCUPAM_HORARIO).stream()
                .filter(agendamento -> !agendamento.getId().equals(ignorar))
                .map(agendamento -> new Intervalo(agendamento.getDataHora(), agendamento.getTerminaEm()));
        Stream<Intervalo> bloqueios = bloqueioRepository.sobrepostos(barbeiro.getId(), de, ate).stream()
                .map(bloqueio -> new Intervalo(bloqueio.getInicio(), bloqueio.getFim()));

        return Stream.concat(agendamentos, bloqueios).toList();
    }
}
