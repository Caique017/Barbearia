package com.barbearia.api.controller;

import com.barbearia.api.dto.senha.EsqueciSenhaRequest;
import com.barbearia.api.dto.senha.RedefinirSenhaRequest;
import com.barbearia.api.service.RedefinicaoSenhaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/senha")
@RequiredArgsConstructor
public class RedefinicaoSenhaController {

    private final RedefinicaoSenhaService redefinicaoSenhaService;

    @PostMapping("/esqueci")
    public ResponseEntity<Void> esqueci(@RequestBody @Valid EsqueciSenhaRequest request) {
        redefinicaoSenhaService.solicitar(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/redefinir")
    public ResponseEntity<Void> redefinir(@RequestBody @Valid RedefinirSenhaRequest request) {
        redefinicaoSenhaService.redefinir(request.token(), request.novaSenha());
        return ResponseEntity.noContent().build();
    }
}
