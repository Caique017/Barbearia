package com.barbearia.api.controller;

import com.barbearia.api.domain.Usuario;
import com.barbearia.api.dto.conta.DadosContaResponse;
import com.barbearia.api.dto.conta.DescadastroRequest;
import com.barbearia.api.dto.conta.ExclusaoContaRequest;
import com.barbearia.api.dto.conta.PreferenciasRequest;
import com.barbearia.api.dto.conta.PreferenciasResponse;
import com.barbearia.api.service.ContaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/conta")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ContaController {

    private final ContaService contaService;

    @GetMapping("/dados")
    public ResponseEntity<DadosContaResponse> exportar(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"meus-dados-clipbook.json\"")
                .body(contaService.exportar(usuario));
    }

    @PostMapping("/excluir")
    public ResponseEntity<Void> excluir(@AuthenticationPrincipal Usuario usuario,
                                        @RequestBody @Valid ExclusaoContaRequest request) {
        contaService.excluir(usuario, request.senha());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preferencias")
    public ResponseEntity<PreferenciasResponse> preferencias(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(contaService.preferencias(usuario));
    }

    @PatchMapping("/preferencias")
    public ResponseEntity<PreferenciasResponse> atualizarPreferencias(@AuthenticationPrincipal Usuario usuario,
                                                                      @RequestBody PreferenciasRequest request) {
        return ResponseEntity.ok(contaService.atualizarPreferencias(usuario, request));
    }

    @PostMapping("/descadastro")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Void> descadastrar(@RequestBody @Valid DescadastroRequest request) {
        contaService.descadastrar(request.token());
        return ResponseEntity.noContent().build();
    }
}
