package com.barbearia.api.controller;

import com.barbearia.api.domain.Usuario;
import com.barbearia.api.dto.barbeiro.BarbeiroPerfilResponse;
import com.barbearia.api.dto.barbeiro.BarbeiroPublicoResponse;
import com.barbearia.api.service.BarbeiroService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/barbeiros")
@RequiredArgsConstructor
@PreAuthorize("hasRole('BARBEIRO')")
public class BarbeiroController {

    private final BarbeiroService barbeiroService;

    @GetMapping("/me")
    public ResponseEntity<BarbeiroPerfilResponse> meuPerfil(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(barbeiroService.buscarPerfil(usuario));
    }

    @PreAuthorize("permitAll()")
    @GetMapping("/publico/{slug}")
    public ResponseEntity<BarbeiroPublicoResponse> buscarPublico(@PathVariable String slug) {
        return ResponseEntity.ok(barbeiroService.buscarPublico(slug));
    }
}
