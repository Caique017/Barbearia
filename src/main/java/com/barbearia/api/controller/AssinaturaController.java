package com.barbearia.api.controller;

import com.barbearia.api.domain.Usuario;
import com.barbearia.api.dto.assinatura.AssinaturaResponse;
import com.barbearia.api.dto.assinatura.CheckoutRequest;
import com.barbearia.api.dto.assinatura.RedirecionamentoResponse;
import com.barbearia.api.service.AssinaturaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('BARBEIRO')")
public class AssinaturaController {

    private final AssinaturaService assinaturaService;

    @GetMapping("/assinatura")
    public ResponseEntity<AssinaturaResponse> consultar(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(assinaturaService.consultar(usuario));
    }

    @PostMapping("/assinatura/checkout")
    public ResponseEntity<RedirecionamentoResponse> checkout(@AuthenticationPrincipal Usuario usuario,
                                                             @RequestBody @Valid CheckoutRequest request) {
        return ResponseEntity.ok(new RedirecionamentoResponse(
                assinaturaService.abrirPagamento(usuario, request.formaPagamento())));
    }

    @PostMapping("/assinatura/portal")
    public ResponseEntity<RedirecionamentoResponse> portal(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(new RedirecionamentoResponse(assinaturaService.abrirPortal(usuario)));
    }

    @PreAuthorize("permitAll()")
    @PostMapping("/pagamentos/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String payload,
                                        @RequestHeader(name = "Stripe-Signature", required = false) String assinatura) {
        assinaturaService.processarWebhook(payload, assinatura);
        return ResponseEntity.ok().build();
    }
}
