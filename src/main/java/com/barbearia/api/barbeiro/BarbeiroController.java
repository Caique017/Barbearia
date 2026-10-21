package com.barbearia.api.barbeiro;

import com.barbearia.api.auth.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PatchMapping("/me")
    public ResponseEntity<BarbeiroPerfilResponse> atualizarPerfil(
            @AuthenticationPrincipal Usuario usuario,
            @RequestBody @Valid BarbeiroAtualizacaoRequest request) {
        return ResponseEntity.ok(barbeiroService.atualizarPerfil(usuario, request));
    }

    @PatchMapping("/me/slug")
    public ResponseEntity<BarbeiroPerfilResponse> atualizarSlug(
            @AuthenticationPrincipal Usuario usuario,
            @RequestBody @Valid BarbeiroSlugRequest request) {
        return ResponseEntity.ok(barbeiroService.atualizarSlug(usuario, request));
    }

    @PreAuthorize("permitAll()")
    @GetMapping("/publico/{slug}")
    public ResponseEntity<BarbeiroPublicoResponse> buscarPublico(@PathVariable String slug) {
        return ResponseEntity.ok(barbeiroService.buscarPublico(slug));
    }
}
