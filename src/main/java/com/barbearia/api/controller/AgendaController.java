package com.barbearia.api.controller;

import com.barbearia.api.domain.Usuario;
import com.barbearia.api.dto.agendamento.AgendamentoBarbeiroRequest;
import com.barbearia.api.dto.agendamento.AgendamentoResponse;
import com.barbearia.api.dto.agendamento.BloqueioRequest;
import com.barbearia.api.dto.agendamento.BloqueioResponse;
import com.barbearia.api.dto.agendamento.FaixaHorario;
import com.barbearia.api.dto.agendamento.HorariosRequest;
import com.barbearia.api.dto.agendamento.RemarcacaoRequest;
import com.barbearia.api.dto.financeiro.ResumoFinanceiroResponse;
import com.barbearia.api.service.AgendamentoService;
import com.barbearia.api.service.BloqueioService;
import com.barbearia.api.service.FinanceiroService;
import com.barbearia.api.service.HorarioAtendimentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('BARBEIRO')")
public class AgendaController {

    private final AgendamentoService agendamentoService;
    private final HorarioAtendimentoService horarioService;
    private final FinanceiroService financeiroService;
    private final BloqueioService bloqueioService;

    @GetMapping("/barbeiros/me/horarios")
    public ResponseEntity<List<FaixaHorario>> horarios(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(horarioService.listar(usuario));
    }

    @PutMapping("/barbeiros/me/horarios")
    public ResponseEntity<List<FaixaHorario>> definirHorarios(@AuthenticationPrincipal Usuario usuario,
                                                              @RequestBody @Valid HorariosRequest request) {
        return ResponseEntity.ok(horarioService.substituir(usuario, request));
    }

    @GetMapping("/barbeiros/me/bloqueios")
    public ResponseEntity<List<BloqueioResponse>> bloqueios(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(bloqueioService.listarFuturos(usuario));
    }

    @PostMapping("/barbeiros/me/bloqueios")
    public ResponseEntity<BloqueioResponse> bloquear(@AuthenticationPrincipal Usuario usuario,
                                                     @RequestBody @Valid BloqueioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bloqueioService.criar(usuario, request));
    }

    @DeleteMapping("/barbeiros/me/bloqueios/{id}")
    public ResponseEntity<Void> desbloquear(@AuthenticationPrincipal Usuario usuario, @PathVariable UUID id) {
        bloqueioService.remover(usuario, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/agenda")
    public ResponseEntity<List<AgendamentoResponse>> agenda(
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return ResponseEntity.ok(agendamentoService.agenda(usuario, de, ate));
    }

    @PostMapping("/agenda")
    public ResponseEntity<AgendamentoResponse> agendar(@AuthenticationPrincipal Usuario usuario,
                                                       @RequestBody @Valid AgendamentoBarbeiroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(agendamentoService.agendarPeloBarbeiro(usuario, request));
    }

    @PostMapping("/agenda/{id}/concluir")
    public ResponseEntity<AgendamentoResponse> concluir(@AuthenticationPrincipal Usuario usuario,
                                                        @PathVariable UUID id) {
        return ResponseEntity.ok(agendamentoService.concluir(usuario, id));
    }

    @PostMapping("/agenda/{id}/falta")
    public ResponseEntity<AgendamentoResponse> falta(@AuthenticationPrincipal Usuario usuario, @PathVariable UUID id) {
        return ResponseEntity.ok(agendamentoService.marcarFalta(usuario, id));
    }

    @PostMapping("/agenda/{id}/cancelar")
    public ResponseEntity<AgendamentoResponse> cancelar(@AuthenticationPrincipal Usuario usuario,
                                                        @PathVariable UUID id) {
        return ResponseEntity.ok(agendamentoService.cancelarPeloBarbeiro(usuario, id));
    }

    @PostMapping("/agenda/{id}/remarcar")
    public ResponseEntity<AgendamentoResponse> remarcar(@AuthenticationPrincipal Usuario usuario,
                                                        @PathVariable UUID id,
                                                        @RequestBody @Valid RemarcacaoRequest request) {
        return ResponseEntity.ok(agendamentoService.remarcarPeloBarbeiro(usuario, id, request.dataHora()));
    }

    @GetMapping("/financeiro/resumo")
    public ResponseEntity<ResumoFinanceiroResponse> resumo(
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return ResponseEntity.ok(financeiroService.resumo(usuario, de, ate));
    }
}
