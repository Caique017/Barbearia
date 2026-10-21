package com.barbearia.api.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificacaoRedefinicaoSenha {

    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoSolicitar(RedefinicaoSenhaSolicitada evento) {
        emailService.enviarRedefinicaoSenha(evento.email(), evento.nome(), evento.link());
    }
}
