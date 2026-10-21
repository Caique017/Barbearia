package com.barbearia.api.controller;

import com.barbearia.api.domain.Usuario;
import com.barbearia.api.dto.servico.ServicoAtualizacaoRequest;
import com.barbearia.api.dto.servico.ServicoPublicoResponse;
import com.barbearia.api.dto.servico.ServicoRequest;
import com.barbearia.api.dto.servico.ServicoResponse;
import com.barbearia.api.service.ServicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('BARBEIRO')")
public class ServicoController {

    private final ServicoService servicoService;

    @PostMapping("/servicos")
    public ResponseEntity<ServicoResponse> criar(@AuthenticationPrincipal Usuario usuario,
                                                 @RequestBody @Valid ServicoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicoService.criar(usuario, request));
    }

    @GetMapping("/servicos")
    public ResponseEntity<List<ServicoResponse>> listar(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(servicoService.listar(usuario));
    }

    @GetMapping("/servicos/{id}")
    public ResponseEntity<ServicoResponse> buscar(@AuthenticationPrincipal Usuario usuario, @PathVariable UUID id) {
        return ResponseEntity.ok(servicoService.buscar(usuario, id));
    }

    @PatchMapping("/servicos/{id}")
    public ResponseEntity<ServicoResponse> atualizar(@AuthenticationPrincipal Usuario usuario,
                                                     @PathVariable UUID id,
                                                     @RequestBody @Valid ServicoAtualizacaoRequest request) {
        return ResponseEntity.ok(servicoService.atualizar(usuario, id, request));
    }

    @PreAuthorize("permitAll()")
    @GetMapping("/barbeiros/publico/{slug}/servicos")
    public ResponseEntity<List<ServicoPublicoResponse>> listarPublicos(@PathVariable String slug) {
        return ResponseEntity.ok(servicoService.listarPublicos(slug));
    }
}
