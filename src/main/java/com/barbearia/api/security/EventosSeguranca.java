package com.barbearia.api.security;

import com.barbearia.api.util.Emails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EventosSeguranca {

    private static final Logger log = LoggerFactory.getLogger("seguranca");

    public void loginFalhou(String email) {
        log.warn("evento=login_falhou email={}", Emails.mascarar(email));
    }

    public void loginBloqueado(String email) {
        log.warn("evento=login_bloqueado email={}", Emails.mascarar(email));
    }

    public void limiteDeRequisicoesExcedido(String regra, String ip) {
        log.warn("evento=limite_ip_excedido regra={} ip={}", regra, ip);
    }

    public void acessoNegado(String metodo, String caminho) {
        log.warn("evento=acesso_negado metodo={} caminho={}", metodo, caminho);
    }

    public void redefinicaoSolicitada(String email) {
        log.info("evento=redefinicao_solicitada email={}", Emails.mascarar(email));
    }

    public void senhaRedefinida(String email) {
        log.info("evento=senha_redefinida email={}", Emails.mascarar(email));
    }

    public void sessoesEncerradas(String email) {
        log.info("evento=sessoes_encerradas email={}", Emails.mascarar(email));
    }
}
