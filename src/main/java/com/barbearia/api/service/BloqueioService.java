package com.barbearia.api.service;

import com.barbearia.api.domain.Barbeiro;
import com.barbearia.api.domain.BloqueioHorario;
import com.barbearia.api.domain.Usuario;
import com.barbearia.api.dto.agendamento.BloqueioRequest;
import com.barbearia.api.dto.agendamento.BloqueioResponse;
import com.barbearia.api.exceptions.BloqueioNaoEncontradoException;
import com.barbearia.api.exceptions.HorarioInvalidoException;
import com.barbearia.api.repositories.AgendamentoRepository;
import com.barbearia.api.repositories.BloqueioHorarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BloqueioService {

    static final int MAXIMO_DIAS = 366;

    private final BloqueioHorarioRepository bloqueioRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final BarbeiroLogado barbeiroLogado;
    private final RelogioLocal relogio;

    @Transactional
    public BloqueioResponse criar(Usuario logado, BloqueioRequest data) {
        Barbeiro barbeiro = barbeiroLogado.de(logado);

        if (!data.inicio().isBefore(data.fim())) {
            throw new HorarioInvalidoException("O fim do bloqueio precisa ser depois do início.");
        }
        if (!data.fim().isAfter(relogio.agora())) {
            throw new HorarioInvalidoException("O bloqueio precisa terminar no futuro.");
        }
        if (ChronoUnit.DAYS.between(data.inicio(), data.fim()) > MAXIMO_DIAS) {
            throw new HorarioInvalidoException("Um bloqueio pode durar no máximo " + MAXIMO_DIAS + " dias.");
        }

        BloqueioHorario bloqueio = bloqueioRepository.save(BloqueioHorario.builder()
                .barbeiro(barbeiro)
                .inicio(data.inicio())
                .fim(data.fim())
                .motivo(data.motivo())
                .build());

        return resposta(bloqueio);
    }

    @Transactional(readOnly = true)
    public List<BloqueioResponse> listarFuturos(Usuario logado) {
        Barbeiro barbeiro = barbeiroLogado.de(logado);

        return bloqueioRepository.findByBarbeiroIdAndFimAfterOrderByInicio(barbeiro.getId(), relogio.agora()).stream()
                .map(this::resposta)
                .toList();
    }

    @Transactional
    public void remover(Usuario logado, UUID id) {
        Barbeiro barbeiro = barbeiroLogado.de(logado);
        BloqueioHorario bloqueio = bloqueioRepository.findByIdAndBarbeiroId(id, barbeiro.getId())
                .orElseThrow(() -> new BloqueioNaoEncontradoException("Bloqueio não encontrado."));
        bloqueioRepository.delete(bloqueio);
    }

    private BloqueioResponse resposta(BloqueioHorario bloqueio) {
        int agendamentos = agendamentoRepository.sobrepostos(bloqueio.getBarbeiro().getId(), bloqueio.getInicio(),
                bloqueio.getFim(), AgendamentoRepository.OCUPAM_HORARIO).size();

        return new BloqueioResponse(bloqueio.getId(), bloqueio.getInicio(), bloqueio.getFim(), bloqueio.getMotivo(),
                agendamentos);
    }
}
