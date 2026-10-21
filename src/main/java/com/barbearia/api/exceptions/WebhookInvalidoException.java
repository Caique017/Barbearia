package com.barbearia.api.exceptions;

public class WebhookInvalidoException extends RuntimeException {
    public WebhookInvalidoException(String mensagem) {
        super(mensagem);
    }
}
