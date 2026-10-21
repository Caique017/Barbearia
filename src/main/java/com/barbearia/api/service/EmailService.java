package com.barbearia.api.service;

import com.barbearia.api.util.Emails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${api.mail.remetente}")
    private String remetente;

    @Value("${api.senha.token-expiracao-minutos:30}")
    private long expiracaoMinutos;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void enviarRedefinicaoSenha(String para, String nome, String link) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(para);
        mensagem.setSubject("Redefinição de senha");
        mensagem.setText("Olá, " + nome + "!\n\n"
                + "Recebemos um pedido para redefinir a sua senha. Para criar uma nova, abra o link abaixo:\n\n"
                + link + "\n\n"
                + "O link vale por " + expiracaoMinutos + " minutos e só pode ser usado uma vez.\n"
                + "Se não foi você que pediu, ignore este e-mail. Sua senha continua a mesma.");

        try {
            mailSender.send(mensagem);
        } catch (MailException e) {
            log.error("Falha ao enviar e-mail de redefinição de senha para {}", Emails.mascarar(para), e);
        }
    }
}
