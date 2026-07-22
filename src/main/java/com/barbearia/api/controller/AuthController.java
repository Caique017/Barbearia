package com.barbearia.api.controller;

import com.barbearia.api.domain.Usuario;
import com.barbearia.api.dto.auth.BarbeiroCadastroRequest;
import com.barbearia.api.dto.auth.BarbeiroCadastroResponse;
import com.barbearia.api.dto.auth.BarbeiroLoginRequest;
import com.barbearia.api.dto.auth.BarbeiroLoginResponse;
import com.barbearia.api.dto.auth.ClienteCadastroRequest;
import com.barbearia.api.dto.auth.ClienteCadastroResponse;
import com.barbearia.api.dto.auth.ClienteLoginRequest;
import com.barbearia.api.dto.auth.ClienteLoginResponse;
import com.barbearia.api.dto.auth.UsuarioLogadoResponse;
import com.barbearia.api.service.BarbeiroService;
import com.barbearia.api.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final BarbeiroService barbeiroService;
    private final ClienteService clienteService;

    @PostMapping("/barbeiro/registro")
    public ResponseEntity<BarbeiroCadastroResponse> registrar(@RequestBody @Valid BarbeiroCadastroRequest request) {
        BarbeiroCadastroResponse response = barbeiroService.registroBarbeiro(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/barbeiro/login")
    public ResponseEntity<BarbeiroLoginResponse> login(@RequestBody @Valid BarbeiroLoginRequest request) {
        BarbeiroLoginResponse response = barbeiroService.loginBarbeiro(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cliente/registro")
    public ResponseEntity<ClienteCadastroResponse> registrarCliente(@RequestBody @Valid ClienteCadastroRequest request) {
        ClienteCadastroResponse response = clienteService.cadastroCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/cliente/login")
    public ResponseEntity<ClienteLoginResponse> loginCliente(@RequestBody @Valid ClienteLoginRequest request) {
        ClienteLoginResponse response = clienteService.loginCliente(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioLogadoResponse> me(@AuthenticationPrincipal Usuario usuario) {
        UsuarioLogadoResponse response = new UsuarioLogadoResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getRole()
        );
        return ResponseEntity.ok(response);
    }
}
