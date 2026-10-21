package com.barbearia.api.controller;

import com.barbearia.api.domain.Usuario;
import com.barbearia.api.dto.agendamento.AgendamentoClienteRequest;
import com.barbearia.api.dto.agendamento.AgendamentoResponse;
import com.barbearia.api.dto.agendamento.DisponibilidadeResponse;
import com.barbearia.api.dto.agendamento.RemarcacaoRequest;
import com.barbearia.api.service.AgendamentoService;
import com.barbearia.api.service.DisponibilidadeService;
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
@PreAuthorize("hasRole('CLIENTE')")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;
    private final DisponibilidadeService disponibilidadeService;

    @PreAuthorize("permitAll()")
    @GetMapping("/barbeiros/publico/{slug}/disponibilidade")
    public ResponseEntity<DisponibilidadeResponse> disponibilidade(
            @PathVariable String slug,
            @RequestParam UUID servicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(disponibilidadeService.doDia(slug, servicoId, data));
    }

    @GetMapping("/agendamentos")
    public ResponseEntity<List<AgendamentoResponse>> meus(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(agendamentoService.meusAgendamentos(usuario));
    }

    @PostMapping("/agendamentos")
    public ResponseEntity<AgendamentoResponse> agendar(@AuthenticationPrincipal Usuario usuario,
                                                       @RequestBody @Valid AgendamentoClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(agendamentoService.agendarComoCliente(usuario, request));
    }

    @PostMapping("/agendamentos/{id}/cancelar")
    public ResponseEntity<AgendamentoResponse> cancelar(@AuthenticationPrincipal Usuario usuario,
                                                        @PathVariable UUID id) {
        return ResponseEntity.ok(agendamentoService.cancelarComoCliente(usuario, id));
    }

    @PostMapping("/agendamentos/{id}/remarcar")
    public ResponseEntity<AgendamentoResponse> remarcar(@AuthenticationPrincipal Usuario usuario,
                                                        @PathVariable UUID id,
                                                        @RequestBody @Valid RemarcacaoRequest request) {
        return ResponseEntity.ok(agendamentoService.remarcarComoCliente(usuario, id, request.dataHora()));
    }
}
