package com.barbearia.api.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailServiceTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);

    private EmailService emailService() {
        EmailService service = new EmailService(mailSender);
        ReflectionTestUtils.setField(service, "remetente", "nao-responda@ex.com");
        ReflectionTestUtils.setField(service, "expiracaoMinutos", 30L);
        return service;
    }

    @Test
    void enviaOLinkParaOEmailDoUsuario() {
        ArgumentCaptor<SimpleMailMessage> mensagem = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService().enviarRedefinicaoSenha("pessoa@ex.com", "Pessoa", "http://front/redefinir-senha?token=abc");

        verify(mailSender).send(mensagem.capture());
        assertThat(mensagem.getValue().getTo()).containsExactly("pessoa@ex.com");
        assertThat(mensagem.getValue().getFrom()).isEqualTo("nao-responda@ex.com");
        assertThat(mensagem.getValue().getText())
                .contains("http://front/redefinir-senha?token=abc")
                .contains("30 minutos");
    }

    @Test
    void falhaNoEnvioNaoSobeExcecao() {
        doThrow(new MailSendException("servidor fora")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> emailService().enviarRedefinicaoSenha("pessoa@ex.com", "Pessoa", "link"))
                .doesNotThrowAnyException();
    }
}
