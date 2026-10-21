package com.barbearia.api.auth;

import com.barbearia.api.barbeiro.BarbeiroCadastroRequest;
import com.barbearia.api.barbeiro.BarbeiroCadastroResponse;
import com.barbearia.api.barbeiro.BarbeiroLoginRequest;
import com.barbearia.api.barbeiro.BarbeiroLoginResponse;
import com.barbearia.api.cliente.ClienteCadastroRequest;
import com.barbearia.api.cliente.ClienteCadastroResponse;
import com.barbearia.api.cliente.ClienteLoginRequest;
import com.barbearia.api.cliente.ClienteLoginResponse;
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

    private final AuthService authService;

    @PostMapping("/barbeiro/registro")
    public ResponseEntity<BarbeiroCadastroResponse> registrar(@RequestBody @Valid BarbeiroCadastroRequest request) {
        BarbeiroCadastroResponse response = authService.cadastroBarbeiro(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/barbeiro/login")
    public ResponseEntity<BarbeiroLoginResponse> login(@RequestBody @Valid BarbeiroLoginRequest request) {
        BarbeiroLoginResponse response = authService.loginBarbeiro(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cliente/registro")
    public ResponseEntity<ClienteCadastroResponse> registrarCliente(@RequestBody @Valid ClienteCadastroRequest request) {
        ClienteCadastroResponse response = authService.cadastroCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/cliente/login")
    public ResponseEntity<ClienteLoginResponse> loginCliente(@RequestBody @Valid ClienteLoginRequest request) {
        ClienteLoginResponse response = authService.loginCliente(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sair")
    public ResponseEntity<Void> sair(@AuthenticationPrincipal Usuario usuario) {
        authService.encerrarSessoes(usuario);
        return ResponseEntity.noContent().build();
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
