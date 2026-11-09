package com.barbearia.api.security;

import com.barbearia.api.enums.Role;
import com.barbearia.api.util.Emails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

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

    public void dadosExportados(String email) {
        log.info("evento=dados_exportados email={}", Emails.mascarar(email));
    }

    public void contaExcluida(String email, Role tipo) {
        log.info("evento=conta_excluida tipo={} email={}", tipo, Emails.mascarar(email));
    }

    public void clienteAnonimizado(UUID clienteId) {
        log.info("evento=cliente_anonimizado cliente={}", clienteId);
    }
}
