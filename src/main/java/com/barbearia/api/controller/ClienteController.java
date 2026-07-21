package com.barbearia.api.controller;

import com.barbearia.api.domain.Usuario;
import com.barbearia.api.dto.cliente.ClienteRapidoRequest;
import com.barbearia.api.dto.cliente.ClienteResumoResponse;
import com.barbearia.api.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('BARBEIRO')")
public class ClienteController {

    private final ClienteService clienteService;

    @PostMapping
    public ResponseEntity<ClienteResumoResponse> cadastrarPeloBarbeiro(
            @AuthenticationPrincipal Usuario usuario,
            @RequestBody @Valid ClienteRapidoRequest request) {
        ClienteResumoResponse response = clienteService.cadastrarPeloBarbeiro(usuario, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}