package com.barbearia.api.auth;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificacaoRedefinicaoSenhaTest {

    @Test
    void repassaOEventoParaOEmail() {
        EmailService emailService = mock(EmailService.class);

        new NotificacaoRedefinicaoSenha(emailService)
                .aoSolicitar(new RedefinicaoSenhaSolicitada("a@ex.com", "Ana", "http://link"));

        verify(emailService).enviarRedefinicaoSenha("a@ex.com", "Ana", "http://link");
    }

    @Test
    void soDisparaDepoisDoCommit() throws NoSuchMethodException {
        TransactionalEventListener listener = NotificacaoRedefinicaoSenha.class
                .getMethod("aoSolicitar", RedefinicaoSenhaSolicitada.class)
                .getAnnotation(TransactionalEventListener.class);

        assertThat(listener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }
}
